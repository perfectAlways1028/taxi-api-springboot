package com.rubyride.tripmanager.security;

import com.rubyride.model.*;
import com.rubyride.tripmanager.exception.UserIdMissingException;
import com.rubyride.tripmanager.repository.mongo.PartnerRepository;
import com.rubyride.tripmanager.repository.mongo.PartnerTransportationRequestRepository;
import com.rubyride.tripmanager.repository.redis.ShiftRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.utility.SpringContext;
import com.rubyride.tripmanager.utility.TripUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component("accessControl")
public class AccessControl {
  private final SpringContext springContext;
  private final PartnerRepository partnerRepository;
  private final PartnerTransportationRequestRepository partnerTransportationRequestRepository;
  private final ShiftRepository shiftRepository;
  private final TripRepository tripRepository;
  private final TripUtils tripUtils;

  @Autowired
  public AccessControl(final SpringContext springContext, final PartnerTransportationRequestRepository partnerTransportationRequestRepository, final ShiftRepository shiftRepository, final TripRepository tripRepository, final PartnerRepository partnerRepository, final TripUtils tripUtils) {
    this.springContext = springContext;
    this.partnerTransportationRequestRepository = partnerTransportationRequestRepository;
    this.partnerRepository = partnerRepository;
    this.shiftRepository = shiftRepository;
    this.tripRepository = tripRepository;
    this.tripUtils = tripUtils;
  }

  public boolean isUser(final UUID userId) {
    if (userId == null) {
      throw new UserIdMissingException("No user id passed in");
    }
    return isUser(Optional.ofNullable(userId));
  }

  public boolean isUser(final Optional<UUID> userId) {
    return userId
        .equals(springContext.getAuthenticatedUserId());
  }

  public boolean canAccessUsers() {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER, Role.DRIVER));
  }

  public boolean canReadUser(final UUID userId) {
    return isUser(userId) ||
        CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
            List.of(Role.ADMIN, Role.DISPATCHER, Role.DRIVER));
  }

  public boolean canWriteUser(final UUID userId) {
    return isUser(userId) ||
        springContext.getAuthenticatedUserRoles().contains(Role.ADMIN);
  }

  public boolean canAccessTripRequests() {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER));
  }

  public boolean canCreateTripRequest(final TripRequest tripRequest) {
    final var roles = springContext.getAuthenticatedUserRoles();

    if (CollectionUtils.containsAny(roles, List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    /* TODO: For now, allow if rider ID matches authenticated user - will eventually enforce
      based on rider's status as far as membership level (or paid for individual trip, etc.) */
    return isUser(tripRequest.getRiderId());
  }

  public boolean canAccessTripRequest(final UUID tripId) {
    return (CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER))) ||
        tripRepository.findById(tripId)
            .map(trip -> isUser(trip.getRiderId()) ||
                isUser(shiftRepository.findById(trip.getShiftId())
                    .map(Shift::getDriverId)))
            .orElse(false);
  }

  public boolean canAccessShifts() {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER));
  }

  public boolean canAccessShift(final UUID shiftId) {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER)) ||
        isUser(shiftRepository.findById(shiftId)
            .map(Shift::getDriverId));
  }

  public boolean canAccessLocations() {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER));
  }

  public boolean canAccessLocation(final UUID driverId) {
    if (CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    return isUser(driverId) ||
        springContext.getAuthenticatedUserId()
            .map(userId ->
                Optional.ofNullable(tripUtils.getActiveTripForRider(userId, OffsetDateTime.now()))
                    .flatMap(tripRequest -> shiftRepository.findById(tripRequest.getShiftId()))
                    .map(Shift::getDriverId)
                    .equals(Optional.ofNullable(driverId)))
            .orElse(false);
  }

  public boolean canReadPartnerTransportationRequests(final UUID partnerId) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (CollectionUtils.containsAny(authenticatedRoles, List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    if (!CollectionUtils.containsAny(authenticatedRoles, List.of(Role.PARTNER, Role.PARTNER_CSR))) {
      return false;
    }

    return springContext.getAuthenticatedUser()
        .map(User::getPartnerId)
        .equals(Optional.of(partnerId));
  }

  public boolean canReadPartnerTransportationRequest(final PartnerTransportationRequest transportationRequest) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (CollectionUtils.containsAny(authenticatedRoles, List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    if (!CollectionUtils.containsAny(authenticatedRoles, List.of(Role.PARTNER, Role.PARTNER_CSR))) {
      return false;
    }

    return partnerRepository.findByFacilitiesContains(transportationRequest.getFacilityId())
        .map(Partner::getId)
        .equals(springContext.getAuthenticatedUser()
            .map(User::getPartnerId));
  }

  public boolean canCreatePartnerTransportationRequest(final PartnerTransportationRequest partnerTransportationRequest) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (authenticatedRoles.contains(Role.ADMIN)) {
      return true;
    }

    return (CollectionUtils.containsAny(authenticatedRoles, List.of(Role.PARTNER, Role.PARTNER_CSR))) &&
        springContext.getAuthenticatedUser()
            .flatMap(user -> partnerRepository.findByFacilitiesContains(partnerTransportationRequest.getFacilityId())
                .map(partner -> partner.getId().equals(user.getPartnerId())))
            .orElse(false);
  }

  public boolean canCreateTripRequestFromTransportationRequest(final UUID partnerTransportationRequestId) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    return authenticatedRoles.contains(Role.PARTNER_CSR) &&
        partnerTransportationRequestRepository.findById(partnerTransportationRequestId)
            .flatMap(partnerTransportationRequest -> springContext.getAuthenticatedUser()
                .flatMap(user -> partnerRepository.findByFacilitiesContains(partnerTransportationRequest.getFacilityId())
                    .map(partner -> partner.getId().equals(user.getPartnerId()))))
            .orElse(false);
  }

  public boolean canUpdatePartnerTransportationRequest(final UUID partnerTransportationRequestId) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (authenticatedRoles.contains(Role.ADMIN)) {
      return true;
    }

    return authenticatedRoles.contains(Role.PARTNER_CSR) &&
        partnerTransportationRequestRepository.findById(partnerTransportationRequestId)
            .flatMap(partnerTransportationRequest -> springContext.getAuthenticatedUser()
                .flatMap(user -> partnerRepository.findByFacilitiesContains(partnerTransportationRequest.getFacilityId())
                    .map(partner -> partner.getId().equals(user.getPartnerId()))))
            .orElse(false);
  }

  public boolean canAccessPartners() {
    return springContext.getAuthenticatedUserRoles().contains(Role.ADMIN);
  }

  public boolean canReadPartner(final UUID partnerId) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (CollectionUtils.containsAny(authenticatedRoles, List.of(Role.ADMIN, Role.DISPATCHER))) {
      return true;
    }

    return CollectionUtils.containsAny(authenticatedRoles, List.of(Role.PARTNER, Role.PARTNER_CSR)) &&
        springContext.getAuthenticatedUser()
            .map(user -> partnerId.equals(user.getPartnerId()))
            .orElse(false);
  }

  public boolean canWritePartner(final UUID partnerId) {
    final var authenticatedRoles = springContext.getAuthenticatedUserRoles();

    if (authenticatedRoles.contains(Role.ADMIN)) {
      return true;
    }

    return authenticatedRoles.contains(Role.PARTNER) &&
        springContext.getAuthenticatedUser()
            .map(user -> partnerId.equals(user.getPartnerId()))
            .orElse(false);
  }

  public boolean canAccessTripSchedulingExceptions() {
    return CollectionUtils.containsAny(springContext.getAuthenticatedUserRoles(),
        List.of(Role.ADMIN, Role.DISPATCHER));
  }

  public boolean canModifyGroups() {
    return springContext.getAuthenticatedUserRoles().contains(Role.ADMIN);
  }

  public boolean canWriteData() {
    return springContext.getAuthenticatedUserRoles().contains(Role.ADMIN);
  }

  public boolean canReadData(final DataType dataType) {
    return springContext.getAuthenticatedUserRoles().contains(Role.ADMIN) ||
        dataType == DataType.PLACE_ICON ||
        dataType == DataType.PLACE_IMAGE;
  }
}
