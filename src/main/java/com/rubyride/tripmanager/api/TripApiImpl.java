package com.rubyride.tripmanager.api;

import com.rubyride.model.*;
import com.rubyride.tripmanager.event.ModifyShiftEvent;
import com.rubyride.tripmanager.event.ModifyTripRequestEvent;
import com.rubyride.tripmanager.event.NewTripRequestEvent;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.*;
import com.rubyride.tripmanager.repository.redis.ShiftRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.security.AccessControl;
import com.rubyride.tripmanager.utility.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TripApiImpl {
  private static final Logger log = LogManager.getLogger(TripApiImpl.class);

  private final AccessControl accessControl;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final PartnerTransportationRequestRepository partnerTransportationRequestRepository;
  private final PlaceRepository placeRepository;
  private final ZoneRepository zoneRepository;
  private final ShiftRepository shiftRepository;
  private final TripRepository tripRepository;
  private final TripArchiveRepository tripArchiveRepository;
  private final UserRepository userRepository;
  private final DriverRepository driverRepository;
  private final VehicleRepository vehicleRepository;
  private final TripUtils tripUtils;

  public TripApiImpl(final AccessControl accessControl, final ApplicationEventPublisher applicationEventPublisher, final PartnerTransportationRequestRepository partnerTransportationRequestRepository, final PlaceRepository placeRepository, final ZoneRepository zoneRepository, final ShiftRepository shiftRepository, final TripRepository tripRepository, final TripArchiveRepository tripArchiveRepository, final UserRepository userRepository, final DriverRepository driverRepository, final VehicleRepository vehicleRepository, final TripUtils tripUtils) {
    this.accessControl = accessControl;
    this.applicationEventPublisher = applicationEventPublisher;
    this.partnerTransportationRequestRepository = partnerTransportationRequestRepository;
    this.placeRepository = placeRepository;
    this.zoneRepository = zoneRepository;
    this.shiftRepository = shiftRepository;
    this.tripRepository = tripRepository;
    this.tripArchiveRepository = tripArchiveRepository;
    this.userRepository = userRepository;
    this.driverRepository = driverRepository;
    this.vehicleRepository = vehicleRepository;
    this.tripUtils = tripUtils;
  }

  private TripRequestWithLocationsAndDriverDetails withLocationsAndDriverDetails(final TripRequest tripRequest) {
    if (tripRequest == null) {
      return null;
    }

    final var fromLocationResponse = placeRepository.findById(tripRequest.getFromLocationId());
    final var toLocationResponse = placeRepository.findById(tripRequest.getToLocationId());

    if (fromLocationResponse.isPresent() && toLocationResponse.isPresent()) {
      return new TripRequestWithLocationsAndDriverDetails()
          .id(tripRequest.getId())
          .riderId(tripRequest.getRiderId())
          .fromLocation(fromLocationResponse.get())
          .toLocation(toLocationResponse.get())
          .fromZoneId(fromLocationResponse.get().getZoneId())
          .toZoneId(toLocationResponse.get().getZoneId())
          .shiftId(tripRequest.getShiftId())
          .driverDetails(getDriverDetails(tripRequest)
              .orElse(null))
          .passengerCount(tripRequest.getPassengerCount())
          .tripRequestType(tripRequest.getTripRequestType())
          .primaryTimeConstraint(tripRequest.getPrimaryTimeConstraint())
          .secondaryTimeConstraint(tripRequest.getSecondaryTimeConstraint())
          .leftFloat(tripRequest.getLeftFloat())
          .rightFloat(tripRequest.getRightFloat())
          .partnerTransportationRequestId(tripRequest.getPartnerTransportationRequestId())
          .status(tripRequest.getStatus())
          .specialInstructions(tripRequest.getSpecialInstructions())
          .lastUpdated(tripRequest.getLastUpdated());
    } else {
      return null;
    }
  }

  private void unassignTripFromShift(final TripRequest tripRequest) {
    Optional.ofNullable(tripRequest.getShiftId())
        .flatMap(shiftRepository::findById)
        .ifPresent(shift -> {
          shiftRepository.save(shift
              .trips(StreamUtils.safeStream(shift.getTrips())
                  .filter(tripRequestId -> !tripRequest.getId().equals(tripRequestId))
                  .collect(Collectors.toList()))
              .events(StreamUtils.safeStream(shift.getEvents())
                  .filter(event -> !tripRequest.getId().equals(event.getTripRequestId()))
                  .collect(Collectors.toList())));

          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));
        });
  }

  private void updateAssociatedPartnerTransportationRequest(final TripRequest tripRequest, final PartnerTransportationRequestStatus status) {
    Optional.ofNullable(tripRequest.getPartnerTransportationRequestId())
        .flatMap(partnerTransportationRequestRepository::findById)
        .map(partnerTransportationRequest -> partnerTransportationRequest.status(status))
        .ifPresent(partnerTransportationRequestRepository::save);
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId) and @accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<TripRequest> assignTripToShift(final UUID tripId, final UUID shiftId, @Min(0) @Valid final Integer position) {
    return tripRepository.findById(tripId)
        .map(tripRequest ->
            shiftRepository.findById(shiftId)
                .map(shift -> {
                  if (tripRequest.getShiftId() != null) {
                    if (tripRequest.getShiftId().equals(shiftId)) {
                      // no-op - trip request already assigned to this shift
                      return ResponseEntity.ok(tripRequest);
                    }

                    unassignTripFromShift(tripRequest);
                  }

                  tripRepository.save(tripRequest
                      .status(TripRequestStatus.DRIVER_ASSIGNED)
                      .shiftId(shiftId))
                      .lastUpdated(OffsetDateTime.now());
                  applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));

                  final var trips = ObjectUtils.getOrDefault(shift.getTrips(), new ArrayList<UUID>());

                  if (position != null && position >= 0 && position <= trips.size()) {
                    trips.add(position, tripId);
                  } else {
                    trips.add(tripId);
                  }

                  shift.setTrips(trips);

                  SchedulingUtils.addOrUpdateEvent(shift, new Event()
                          .action(EventAction.PICKUP)
                          .riderId(tripRequest.getRiderId())
                          .passengerDelta(ObjectUtils.getOrDefault(tripRequest.getPassengerCount(), 1))
                          .placeId(tripRequest.getFromLocationId())
                          .leftFloat(tripRequest.getLeftFloat())
                          .rightFloat(tripRequest.getRightFloat())
                          .tripRequestId(tripId),
                      null);

                  SchedulingUtils.addOrUpdateEvent(shift, new Event()
                          .action(EventAction.DROPOFF)
                          .riderId(tripRequest.getRiderId())
                          .passengerDelta(-ObjectUtils.getOrDefault(tripRequest.getPassengerCount(), 1))
                          .placeId(tripRequest.getToLocationId())
                          .tripRequestId(tripId),
                      null);
                  shiftRepository.save(shift);
                  applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));

                  return ResponseEntity.ok(tripRequest);
                })
                .orElseThrow(() -> new EntityNotFoundException("Shift not found"))
        )
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> cancelTripById(final UUID tripId, final TripRequestStatus reason) {
    if (!List
        .of(
            TripRequestStatus.CANCEL_BY_RIDER,
            TripRequestStatus.CANCEL_BY_DRIVER_RIDER_NOT_PRESENT,
            TripRequestStatus.CANCEL_BY_DRIVER_RIDER_LATE)
        .contains(reason)) {
      return ResponseEntity.badRequest()
          .build();
    }

    return tripRepository.findById(tripId)
        .map(tripRequest -> tripRequest.status(reason)
            .lastUpdated(OffsetDateTime.now()))
        .map(tripRequest -> {
          tripRepository.save(tripRequest);
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));

          if (tripRequest.getShiftId() != null) {
            // delete any incomplete events associated with this trip request
            shiftRepository.findById(tripRequest.getShiftId())
                .ifPresent(shift -> {
                  final boolean dataOnly = List.of(
                      TripRequestStatus.CANCEL_BY_DRIVER_RIDER_NOT_PRESENT,
                      TripRequestStatus.CANCEL_BY_DRIVER_RIDER_LATE)
                      .contains(reason);

                  final var events = ObjectUtils.getOrDefault(shift.getEvents(), new ArrayList<Event>());

                  events.removeIf(event ->
                      org.springframework.util.ObjectUtils.nullSafeEquals(tripId, event.getTripRequestId()) &&
                          !Boolean.TRUE.equals(event.getComplete()));
                  shiftRepository.save(shift.events(events));
                  applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, dataOnly));
                });
          }

          updateAssociatedPartnerTransportationRequest(tripRequest, PartnerTransportationRequestStatus.CANCELLED);

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<Void> deleteTrip(final UUID tripId) {
    tripRepository.findById(tripId)
        .map(tripRequest -> tripRequest.status(TripRequestStatus.CANCEL_BY_RIDER))
        .ifPresent(tripRequest -> {
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));

          final var shiftId = tripRequest.getShiftId();

          if (shiftId != null) {
            // remove trip request and associated events from assigned shift
            shiftRepository.findById(tripRequest.getShiftId())
                .ifPresent(shift -> {
                  shiftRepository.save(shift
                      .events(StreamUtils.safeStream(shift.getEvents())
                          .filter(event -> !tripId.equals(event.getTripRequestId()))
                          .collect(Collectors.toList()))
                      .trips(StreamUtils.safeStream(shift.getTrips())
                          .filter(assignedTripId -> !tripId.equals(assignedTripId))
                          .collect(Collectors.toList())));
                  applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest.status(TripRequestStatus.CANCEL_BY_RIDER), null, false));
                });
          }

          updateAssociatedPartnerTransportationRequest(tripRequest, PartnerTransportationRequestStatus.CANCELLED);

          tripRepository.deleteById(tripId);
        });

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<List<TripRequest>> getAllTrips() {
    return ResponseEntity.ok(StreamUtils.streamIterable(tripRepository.findAll())
        .filter(tripRequest -> accessControl.canAccessTripRequest(tripRequest.getId()))
        .collect(Collectors.toList()));
  }

  public ResponseEntity<List<TripRequest>> getArchivedTrips(final LocalDate from, final LocalDate to, final UUID zoneId, final UUID riderId) {
    final var dates = DateUtils.getMinMaxDates(from, to);
    final Stream<TripRequest> tripRequests;

    if (riderId != null) {
      tripRequests = userRepository.findById(riderId)
          .map(user -> {
            if (zoneId != null) {
              return zoneRepository.findById(zoneId)
                  .map(zone -> StreamUtils.merge(tripRepository.findByRiderId(user.getId()).stream(),
                      tripArchiveRepository.findByRiderId(user.getId()).stream())
                      .filter(tripRequest -> tripRequest.getFromZoneId().equals(zone.getId()) ||
                          tripRequest.getToZoneId().equals(zone.getId())))
                  .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
            } else {
              return StreamUtils.merge(tripRepository.findByRiderId(user.getId()).stream(),
                  tripArchiveRepository.findByRiderId(user.getId()).stream());
            }
          })
          .orElseThrow(() -> new EntityNotFoundException("Rider not found"));
    } else {
      if (zoneId != null) {
        tripRequests = zoneRepository.findById(zoneId)
            .map(zone -> StreamUtils.merge(tripRepository.findByFromZoneId(zone.getId()).stream(),
                tripRepository.findByToZoneId(zone.getId()).stream(),
                tripArchiveRepository.findByFromZoneId(zone.getId()).stream(),
                tripArchiveRepository.findByToZoneId(zone.getId()).stream()))
            .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
      } else {
        tripRequests = StreamUtils.merge(StreamUtils.streamIterable(tripRepository.findAll()),
            StreamUtils.streamIterable(tripArchiveRepository.findAll()));
      }
    }

    return ResponseEntity.ok(tripRequests
        .distinct()
        .filter(tripRequest -> accessControl.canAccessTripRequest(tripRequest.getId()))
        .filter(request -> {
          final var requestDate = tripUtils.getPrimaryTimeConstraint(request).toLocalDate();
          return dates.getFirst().compareTo(requestDate) <= 0 &&
              dates.getSecond().compareTo(requestDate) > 0;
        })
        .sorted(Comparator.comparing(tripUtils::getPrimaryTimeConstraint))
        .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canReadUser(#riderId)")
  public ResponseEntity<TripRequestWithLocationsAndDriverDetails> getActiveTrip(final UUID riderId) {
    return ResponseEntity.of(Optional.ofNullable(withLocationsAndDriverDetails(tripUtils.getActiveTripForRider(riderId, OffsetDateTime.now()))));
  }

  @PreAuthorize("@accessControl.canReadUser(#riderId)")
  public ResponseEntity<List<TripRequestWithLocationsAndDriverDetails>> getUpcomingTrips(final UUID riderId) {
    final var now = OffsetDateTime.now();
    final var activeTripId = Optional.ofNullable(tripUtils.getActiveTripForRider(riderId, now))
        .map(TripRequest::getId);

    return ResponseEntity.ok(StreamUtils.streamIterable(tripRepository.findByRiderId(riderId))
        .filter(tripRequest -> !Optional.of(tripRequest.getId()).equals(activeTripId))
        .filter(tripRequest -> tripUtils.getPrimaryTimeConstraint(tripRequest).isAfter(now))
        .filter(tripRequest -> tripRequest.getStatus() != TripRequestStatus.TRIP_COMPLETE)
        .sorted(Comparator.nullsLast(Comparator.comparing(tripUtils::getPrimaryTimeConstraint)))
        .map(this::withLocationsAndDriverDetails)
        .collect(Collectors.toList()));
  }

  public ResponseEntity<List<TripRequest>> getTrip(final List<UUID> tripId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(tripRepository.findAllById(tripId))
        .filter(tripRequest -> accessControl.canAccessTripRequest(tripRequest.getId()))
        .collect(Collectors.toList()));
  }

  private Optional<DriverDetails> getDriverDetails(final TripRequest tripRequest) {
    return shiftRepository.findById(ObjectUtils.getOrDefault(tripRequest.getShiftId(), UUID.randomUUID()))
        .map(shift -> {
          final var driver = Optional.ofNullable(driverRepository.findByUserId(ObjectUtils.getOrDefault(shift.getDriverId(), UUID.randomUUID())))
              .orElse(new Driver());

          return userRepository.findById(ObjectUtils.getOrDefault(driver.getUserId(), ObjectUtils.getOrDefault(shift.getDriverId(), UUID.randomUUID())))
              .map(user -> {
                final var vehicle = vehicleRepository.findById(ObjectUtils.getOrDefault(driver.getVehicle(), UUID.randomUUID()))
                    .orElse(new Vehicle());

                return Optional.of(new DriverDetails()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPrimaryPhone())
                    .image(driver.getImage())
                    .hireDate(driver.getHireDate())
                    .vaccinationStatus(driver.getVaccinationStatus())
                    .vehicle(vehicle
                        .id(null)
                        .registrationType(null)
                        .registrationExpiration(null)
                        .insuranceType(null)
                        .insuranceExpiration(null)
                        .insuranceNamed(null)
                        .vin(null)));
              })
              .orElse(Optional.empty());
        })
        .orElse(Optional.empty());
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<DriverDetails> getTripDriverDetails(final UUID tripId) {
    return ResponseEntity.of(tripRepository.findById(tripId)
        .flatMap(this::getDriverDetails));
  }

  @PreAuthorize("@accessControl.canReadUser(#driverId)")
  public ResponseEntity<List<TripRequest>> getTripsForDriver(final UUID driverId) {
    // get active shifts for driver
    final List<Shift> driverShifts = StreamUtils.streamIterable(shiftRepository.findByDriverId(driverId))
        .filter(shift -> Boolean.TRUE.equals(shift.getActive()))
        .collect(Collectors.toList());

    final OffsetDateTime now = OffsetDateTime.now();

    final Shift currentActiveShift = driverShifts.stream()
        // prefer newest active shift whose start and end times contain current time
        .filter(shift -> ObjectUtils.getOrDefault(shift.getStartTime(), now).isBefore(now) &&
            ObjectUtils.getOrDefault(shift.getEndTime(), now).isAfter(now))
        .max((a, b) -> {
          if (a.getCreated() == null) {
            return 1;
          } else if (b.getCreated() == null) {
            return -1;
          } else {
            return a.getCreated().compareTo(b.getCreated());
          }
        })
        .orElse(driverShifts.stream()
            // none do; grab newest-created active shift
            .max((a, b) -> {
              if (a.getCreated() == null) {
                return 1;
              } else if (b.getCreated() == null) {
                return -1;
              } else {
                return a.getCreated().compareTo(b.getCreated());
              }
            })
            .orElseThrow(() -> new EntityNotFoundException("No active shift for driver")));

    return ResponseEntity.ok(StreamUtils.streamIterable(
        tripRepository.findAllById(ObjectUtils.getOrDefault(currentActiveShift.getTrips(), Collections.emptyList())))
        .sorted(Comparator.comparing(tripUtils::getPrimaryTimeConstraint))
        .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canAccessTripRequests()")
  public ResponseEntity<List<TripRequest>> getTripsForZone(final UUID zoneId, final LocalDate date) {
    return zoneRepository.findById(zoneId)
        .map(zone -> ResponseEntity.ok(
            StreamUtils.merge(tripRepository.findByFromZoneId(zone.getId()).stream(),
                tripRepository.findByToZoneId(zone.getId()).stream())
                .unordered()
                .distinct()
                .filter(trip -> date == null || (
                    date.compareTo(tripUtils.getPrimaryTimeConstraint(trip).atZoneSameInstant(ZoneOffset.ofHours(zone.getTimeZone())).toLocalDate()) == 0))
                .collect(Collectors.toList())))
        .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
  }

  @PreAuthorize("@accessControl.canCreateTripRequest(#tripRequest)")
  public ResponseEntity<TripRequest> requestTrip(@Valid final TripRequest tripRequest) {
    // Validate trip - this will throw an exception if it fails
    final var tripRequestWithZones = tripUtils.validateTripRequest(tripRequest);
    final var now = OffsetDateTime.now();

    tripRepository.save(tripRequestWithZones
        .id(UUID.randomUUID())
        .status(ObjectUtils.getOrDefault(tripRequestWithZones.getStatus(), TripRequestStatus.NEW))
        .created(now)
        .lastUpdated(now));

    final var newTripRequest = tripRequestWithZones.getShiftId() != null ?
        assignTripToShift(tripRequestWithZones.getId(), tripRequestWithZones.getShiftId(), null)
            .getBody() :
        tripRequestWithZones;
    applicationEventPublisher.publishEvent(new NewTripRequestEvent(newTripRequest));

    return ResponseEntity.created(URI.create("/v1/trips/" + newTripRequest.getId().toString()))
        .body(newTripRequest);
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> tripEnroute(final UUID tripId, @Valid final LatitudeLongitude location) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          tripRepository.save(tripRequest
              .status(TripRequestStatus.DRIVER_EN_ROUTE)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));

          Optional.ofNullable(tripRequest.getShiftId())
              .ifPresentOrElse(shiftId -> shiftRepository.findById(shiftId)
                  .ifPresentOrElse(shift -> {
                    shiftRepository.save(SchedulingUtils.addOrUpdateEvent(shift, new Event()
                        .action(EventAction.DRIVER_EN_ROUTE)
                        .time(OffsetDateTime.now())
                        .riderId(tripRequest.getRiderId())
                        .placeId(tripRequest.getFromLocationId())
                        .tripRequestId(tripId)
                        .location(location)
                        .complete(true), null));
                    applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, true));
                  }, () -> log.warn("Shift " + shiftId + " associated with trip request " + tripId + " not found")), () -> log.warn("No shift associated with trip ID " + tripId));

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> tripPickupArrived(final UUID tripId, @Valid final LatitudeLongitude location) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          tripRepository.save(tripRequest
              .status(TripRequestStatus.DRIVER_ARRIVED)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, EventAction.PICKUP_ARRIVAL));

          Optional.ofNullable(tripRequest.getShiftId())
              .ifPresentOrElse(shiftId -> shiftRepository.findById(shiftId)
                  .ifPresentOrElse(shift -> {
                    shiftRepository.save(SchedulingUtils.addOrUpdateEvent(shift, new Event()
                        .action(EventAction.PICKUP_ARRIVAL)
                        .time(OffsetDateTime.now())
                        .riderId(tripRequest.getRiderId())
                        .placeId(tripRequest.getFromLocationId())
                        .tripRequestId(tripId)
                        .location(location)
                        .complete(true), null));
                    applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));
                  }, () -> log.warn("Shift " + shiftId + " associated with trip request " + tripId + " not found")), () -> log.warn("No shift associated with trip ID " + tripId));

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> tripPickUpComplete(final UUID tripId, @Valid final LatitudeLongitude location) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          tripRepository.save(tripRequest
              .status(TripRequestStatus.TRIP_IN_PROGRESS)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, EventAction.PICKUP));

          Optional.ofNullable(tripRequest.getShiftId())
              .ifPresentOrElse(shiftId -> shiftRepository.findById(shiftId)
                  .ifPresentOrElse(shift -> {
                    StreamUtils.safeStream(shift.getEvents())
                        .filter(event -> event.getTripRequestId() != null &&
                            tripId.equals(event.getTripRequestId()) &&
                            event.getAction().equals(EventAction.PICKUP))
                        .forEach(event -> {
                          event.setTime(OffsetDateTime.now());
                          event.setLocation(location);
                          event.setComplete(true);
                        });

                    shiftRepository.save(SchedulingUtils.sortEvents(shift));
                    applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));
                  }, () -> log.warn("Shift " + shiftId + " associated with trip request " + tripId + " not found")), () -> log.warn("No shift associated with trip ID " + tripId));

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> tripDropffArrived(final UUID tripId, @Valid final LatitudeLongitude location) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          tripRepository.save(tripRequest
              .status(TripRequestStatus.TRIP_IN_PROGRESS)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, EventAction.DROPOFF_ARRIVAL));

          Optional.ofNullable(tripRequest.getShiftId())
              .ifPresentOrElse(shiftId -> shiftRepository.findById(shiftId)
                  .ifPresentOrElse(shift -> {
                    shiftRepository.save(SchedulingUtils.addOrUpdateEvent(shift, new Event()
                        .action(EventAction.DROPOFF_ARRIVAL)
                        .time(OffsetDateTime.now())
                        .riderId(tripRequest.getRiderId())
                        .placeId(tripRequest.getToLocationId())
                        .tripRequestId(tripId)
                        .location(location)
                        .complete(true), null));

                    shiftRepository.save(SchedulingUtils.sortEvents(shift));
                    applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));
                  }, () -> log.warn("Shift " + shiftId + " associated with trip request " + tripId + " not found")), () -> log.warn("No shift associated with trip ID " + tripId));

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> tripDropOffComplete(final UUID tripId, @Valid final LatitudeLongitude location) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          tripRepository.save(tripRequest
              .status(TripRequestStatus.TRIP_COMPLETE)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, EventAction.DROPOFF));

          Optional.ofNullable(tripRequest.getShiftId())
              .ifPresentOrElse(shiftId -> shiftRepository.findById(shiftId)
                      .ifPresentOrElse(shift -> {
                            StreamUtils.safeStream(shift.getEvents())
                                .filter(event -> event.getTripRequestId() != null &&
                                    tripId.equals(event.getTripRequestId()) &&
                                    event.getAction().equals(EventAction.DROPOFF))
                                .forEach(event -> {
                                  event.setTime(OffsetDateTime.now());
                                  event.setLocation(location);
                                  event.setComplete(true);
                                });

                            shiftRepository.save(SchedulingUtils.sortEvents(shift));
                            applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, tripRequest, null, false));
                          },
                          () -> log.warn("Shift " + shiftId + " associated with trip request " + tripId + " not found")),
                  () -> log.warn("No shift associated with trip ID " + tripId));

          updateAssociatedPartnerTransportationRequest(tripRequest, PartnerTransportationRequestStatus.COMPLETE);

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripRequest.id)")
  public ResponseEntity<TripRequest> updateTrip(@Valid final TripRequest tripRequest) {
    return tripRepository.findById(tripRequest.getId())
        .map(existingTripRequest -> {
          if (tripRequest.getRiderId() != null) {
            existingTripRequest.setRiderId(tripRequest.getRiderId());
          }

          if (tripRequest.getFromLocationId() != null) {
            existingTripRequest.setFromLocationId(tripRequest.getFromLocationId());
          }

          if (tripRequest.getFromLocationNote() != null) {
            existingTripRequest.setFromLocationNote(tripRequest.getFromLocationNote());
          }

          if (tripRequest.getToLocationId() != null) {
            existingTripRequest.setToLocationId(tripRequest.getToLocationId());
          }

          if (tripRequest.getToLocationNote() != null) {
            existingTripRequest.setToLocationNote(tripRequest.getToLocationNote());
          }

          if (tripRequest.getShiftId() != null) {
            existingTripRequest.setShiftId(tripRequest.getShiftId());
          }

          if (tripRequest.getPassengerCount() != null) {
            existingTripRequest.setPassengerCount(tripRequest.getPassengerCount());
          }

          if (tripRequest.getTripRequestType() != null) {
            existingTripRequest.setTripRequestType(tripRequest.getTripRequestType());
          }

          if (tripRequest.getPrimaryTimeConstraint() != null) {
            existingTripRequest.setPrimaryTimeConstraint(tripRequest.getPrimaryTimeConstraint());
          }

          if (tripRequest.getSecondaryTimeConstraint() != null) {
            existingTripRequest.setSecondaryTimeConstraint(tripRequest.getSecondaryTimeConstraint());
          }

          if (tripRequest.getLeftFloat() != null) {
            existingTripRequest.setLeftFloat(tripRequest.getLeftFloat());
          }

          if (tripRequest.getRightFloat() != null) {
            existingTripRequest.setRightFloat(tripRequest.getRightFloat());
          }

          if (tripRequest.getStatus() != null) {
            existingTripRequest.setStatus(tripRequest.getStatus());
          }

          if (tripRequest.getScheduleType() != null) {
            existingTripRequest.setScheduleType(tripRequest.getScheduleType());
          }

          if (tripRequest.getPartnerTransportationRequestId() != null) {
            existingTripRequest.setPartnerTransportationRequestId(tripRequest.getPartnerTransportationRequestId());
          }

          if (tripRequest.getSpecialInstructions() != null) {
            existingTripRequest.setSpecialInstructions(tripRequest.getSpecialInstructions());
          }

          existingTripRequest.setLastUpdated(OffsetDateTime.now());

          final var existingTripRequestWithUpdatedZones = tripUtils.setZones(existingTripRequest);

          tripRepository.save(existingTripRequestWithUpdatedZones);
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + existingTripRequest.getId().toString()))
              .body(existingTripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }

  @PreAuthorize("@accessControl.canAccessTripRequest(#tripId)")
  public ResponseEntity<TripRequest> setTripNeedsAssigned(final UUID tripId) {
    return tripRepository.findById(tripId)
        .map(tripRequest -> {
          unassignTripFromShift(tripRequest);

          tripRepository.save(tripRequest
              .status(TripRequestStatus.NEEDS_ASSIGNMENT)
              .shiftId(null)
              .lastUpdated(OffsetDateTime.now()));
          applicationEventPublisher.publishEvent(new ModifyTripRequestEvent(tripRequest, null));
          log.warn("Trip request " + tripId + " set to Needs_Assignment.");

          return ResponseEntity.ok()
              .location(URI.create("/v1/trips/" + tripRequest.getId().toString()))
              .body(tripRequest);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
  }
}
