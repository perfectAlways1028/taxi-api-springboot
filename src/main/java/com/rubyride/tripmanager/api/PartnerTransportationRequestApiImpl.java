package com.rubyride.tripmanager.api;

import com.rubyride.model.*;
import com.rubyride.tripmanager.event.NewTripRequestEvent;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.PartnerRepository;
import com.rubyride.tripmanager.repository.mongo.PartnerTransportationRequestRepository;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.security.AccessControl;
import com.rubyride.tripmanager.service.MapService;
import com.rubyride.tripmanager.utility.ObjectUtils;
import com.rubyride.tripmanager.utility.StreamUtils;
import com.rubyride.tripmanager.utility.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PartnerTransportationRequestApiImpl {
  private final AccessControl accessControl;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final PartnerRepository partnerRepository;
  private final PlaceRepository placeRepository;
  private final TripRepository tripRepository;
  private final UserRepository userRepository;
  private final PartnerTransportationRequestRepository partnerTransportationRequestRepository;
  private final MapService mapService;
  private final UserUtils userUtils;

  @Autowired
  public PartnerTransportationRequestApiImpl(final AccessControl accessControl, final ApplicationEventPublisher applicationEventPublisher, final PartnerRepository partnerRepository, final PlaceRepository placeRepository, final TripRepository tripRepository, final UserRepository userRepository, final PartnerTransportationRequestRepository partnerTransportationRequestRepository, final MapService mapService, final UserUtils userUtils) {
    this.accessControl = accessControl;
    this.applicationEventPublisher = applicationEventPublisher;
    this.partnerRepository = partnerRepository;
    this.placeRepository = placeRepository;
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
    this.partnerTransportationRequestRepository = partnerTransportationRequestRepository;
    this.mapService = mapService;
    this.userUtils = userUtils;
  }

  @PreAuthorize("@accessControl.canUpdatePartnerTransportationRequest(#transportationRequestId)")
  public ResponseEntity<Void> deletePartnerTransportationRequest(final UUID transportationRequestId) {
    partnerTransportationRequestRepository.deleteById(transportationRequestId);

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<List<PartnerTransportationRequest>> getPartnerTransportationRequest(final List<UUID> transportationRequestId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(partnerTransportationRequestRepository.findAllById(transportationRequestId))
        .filter(accessControl::canReadPartnerTransportationRequest)
        .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canReadPartnerTransportationRequests(#partnerId)")
  public ResponseEntity<List<PartnerTransportationRequest>> getPartnerTransportationRequestByPartner(final UUID partnerId) {
    final var partner = partnerRepository.findById(partnerId)
        .orElseThrow(() -> new EntityNotFoundException("Partner not found"));

    return ResponseEntity.ok(
        partnerTransportationRequestRepository.findByFacilityIdIn(
            ObjectUtils.getOrDefault(partner.getFacilities(), Collections.emptySet())));
  }

  @PreAuthorize("@accessControl.canCreatePartnerTransportationRequest(#partnerTransportationRequest)")
  public ResponseEntity<PartnerTransportationRequest> requestPartnerTransportationRequest(@Valid final PartnerTransportationRequest partnerTransportationRequest) {
    final var newPartnerTransportationRequest = partnerTransportationRequest
        .id(UUID.randomUUID())
        .created(OffsetDateTime.now())
        .lastUpdated(OffsetDateTime.now());

    partnerTransportationRequestRepository.save(newPartnerTransportationRequest);

    return ResponseEntity.created(URI.create("/v1/partnerTransportationRequests/" + newPartnerTransportationRequest.getId()))
        .body(newPartnerTransportationRequest);
  }

  @PreAuthorize("@accessControl.canCreateTripRequestFromTransportationRequest(#transportationRequestId)")
  public ResponseEntity<PartnerTransportationRequest> scheduleTripFromPartnerTransportationRequest(final UUID transportationRequestId, @Valid final TimeConstraint timeConstraint) {
    return partnerTransportationRequestRepository.findById(transportationRequestId)
        .map(transportationRequest -> {
          final var facility = placeRepository.findById(transportationRequest.getFacilityId())
              .orElseThrow(() -> new EntityNotFoundException("Facility not found"));

          final var userId = UUID.randomUUID();

          final var homePlace = mapService.geocode(new Place()
              .id(UUID.randomUUID())
              .address(transportationRequest.getAddress())
              .name("Home")
              .isPrivate(true)
              .userId(userId));

          final var user = new User()
              .id(userId)
              .firstName(transportationRequest.getFirstName())
              .lastName(transportationRequest.getLastName())
              .userName(userUtils.generateUsername(transportationRequest.getFirstName(), transportationRequest.getLastName()))
              .email(transportationRequest.getEmail())
              .address(transportationRequest.getAddress())
              .primaryPhone(transportationRequest.getPrimaryPhone())
              .zones(Stream.of(homePlace.getZoneId(), facility.getZoneId())
                  .filter(Objects::nonNull)
                  .collect(Collectors.toSet()))
              .notificationType(NotificationType.SMS)
              .roles(List.of(Role.RIDER))
              .active(Boolean.TRUE)
              .created(OffsetDateTime.now());

          final var tripRequest = new TripRequest()
              .id(UUID.randomUUID())
              .riderId(user.getId())
              .partnerTransportationRequestId(transportationRequest.getId())
              .tripRequestType(TripRequestType.PASSENGER)
              .passengerCount(1)
              .created(OffsetDateTime.now())
              .primaryTimeConstraint(timeConstraint)
              .fromLocationId(homePlace.getId())
              .fromZoneId(homePlace.getZoneId())
              .toLocationId(facility.getId())
              .toZoneId(facility.getZoneId())
              .status(TripRequestStatus.NEW);

          final var returnTripRequest = new TripRequest()
              .id(UUID.randomUUID())
              .riderId(user.getId())
              .tripRequestType(TripRequestType.PASSENGER)
              .passengerCount(1)
              .created(OffsetDateTime.now())
              .primaryTimeConstraint(new TimeConstraint()
                  .time(timeConstraint.getTime()
                      .plusHours(1L))
                  .constraintType(timeConstraint.getConstraintType()))
              .fromLocationId(facility.getId())
              .fromZoneId(facility.getZoneId())
              .toLocationId(homePlace.getId())
              .toZoneId(homePlace.getZoneId())
              .status(TripRequestStatus.NEW);

          transportationRequest.setTripId(tripRequest.getId());
          transportationRequest.setStatus(PartnerTransportationRequestStatus.SCHEDULED);

          userRepository.save(user);
          placeRepository.save(homePlace);
          tripRepository.save(tripRequest);
          tripRepository.save(returnTripRequest);
          partnerTransportationRequestRepository.save(transportationRequest);

          applicationEventPublisher.publishEvent(new NewTripRequestEvent(tripRequest));
          applicationEventPublisher.publishEvent(new NewTripRequestEvent(returnTripRequest));

          return transportationRequest;
        })
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new EntityNotFoundException("Partner transportation request not found"));
  }

  @PreAuthorize("@accessControl.canUpdatePartnerTransportationRequest(#partnerTransportationRequest.id)")
  public ResponseEntity<PartnerTransportationRequest> updatePartnerTransportationRequest(@Valid final PartnerTransportationRequest partnerTransportationRequest) {
    return partnerTransportationRequestRepository.findById(partnerTransportationRequest.getId())
        .map(existingPartnerTransportationRequest -> {
          if (partnerTransportationRequest.getAddress() != null) {
            existingPartnerTransportationRequest.setAddress(partnerTransportationRequest.getAddress());
          }

          if (partnerTransportationRequest.getEmail() != null) {
            existingPartnerTransportationRequest.setEmail(partnerTransportationRequest.getEmail());
          }

          if (partnerTransportationRequest.getFacilityId() != null) {
            existingPartnerTransportationRequest.setFacilityId(partnerTransportationRequest.getFacilityId());
          }

          if (partnerTransportationRequest.getFirstName() != null) {
            existingPartnerTransportationRequest.setFirstName(partnerTransportationRequest.getFirstName());
          }

          if (partnerTransportationRequest.getLastName() != null) {
            existingPartnerTransportationRequest.setLastName(partnerTransportationRequest.getLastName());
          }

          if (partnerTransportationRequest.getNotes() != null) {
            existingPartnerTransportationRequest.setNotes(partnerTransportationRequest.getNotes());
          }

          if (partnerTransportationRequest.getPrimaryPhone() != null) {
            existingPartnerTransportationRequest.setPrimaryPhone(partnerTransportationRequest.getPrimaryPhone());
          }

          if (partnerTransportationRequest.getStatus() != null) {
            existingPartnerTransportationRequest.setStatus(partnerTransportationRequest.getStatus());
          }

          if (partnerTransportationRequest.getSubmitterUserId() != null) {
            existingPartnerTransportationRequest.setSubmitterUserId(partnerTransportationRequest.getSubmitterUserId());
          }

          if (partnerTransportationRequest.getTripId() != null) {
            existingPartnerTransportationRequest.setTripId(partnerTransportationRequest.getTripId());
          }

          existingPartnerTransportationRequest.setLastUpdated(OffsetDateTime.now());

          partnerTransportationRequestRepository.save(existingPartnerTransportationRequest);

          return ResponseEntity.ok()
              .location(URI.create("/v1/partnerTransportationRequests/" + existingPartnerTransportationRequest.getId()))
              .body(existingPartnerTransportationRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Partner transportation request not found"));
  }
}
