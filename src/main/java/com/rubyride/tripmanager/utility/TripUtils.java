package com.rubyride.tripmanager.utility;

import com.rubyride.model.*;
import com.rubyride.tripmanager.exception.InvalidTripRequestException;
import com.rubyride.tripmanager.repository.mongo.GroupRepository;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TripUtils {
  private final GroupRepository groupRepository;
  private final PlaceRepository placeRepository;
  private final TripRepository tripRepository;
  private final UserRepository userRepository;

  public TripUtils(final GroupRepository groupRepository, final PlaceRepository placeRepository, final TripRepository tripRepository, final UserRepository userRepository) {
    this.groupRepository = groupRepository;
    this.placeRepository = placeRepository;
    this.tripRepository = tripRepository;
    this.userRepository = userRepository;
  }

  public TripRequest setZones(final TripRequest request) {
    return request
        .fromZoneId(placeRepository.findById(request.getFromLocationId())
            .map(Place::getZoneId)
            .orElse(null))
        .toZoneId(placeRepository.findById(request.getToLocationId())
            .map(Place::getZoneId)
            .orElse(null));
  }

  // Verify trip request appears to be valid
  public TripRequest validateTripRequest(final TripRequest tripRequest) throws InvalidTripRequestException {
    // First set to and from zones on trip since that's part of the check
    final var tripRequestWithZones = setZones(tripRequest);

    // Make sure user exists
    final var user = userRepository.findById(tripRequestWithZones.getRiderId())
        .orElseThrow(() -> new InvalidTripRequestException("User does not exist"));

    // Make sure user's group(s) or zone(s) contain to or from location
    // TODO: groups only?
    final var groups = groupRepository.findAllById(
        ObjectUtils.getOrDefault(user.getGroups(), Collections.emptyList()));
    final var zones = ObjectUtils.getOrDefault(user.getZones(), Collections.<UUID>emptyList());

    final var allZones = StreamUtils.merge(StreamUtils.streamIterable(groups)
            .map(Group::getOriginZoneId),
        zones.stream())
        .collect(Collectors.toSet());

    if (!allZones.contains(tripRequestWithZones.getFromZoneId()) &&
        !allZones.contains(tripRequestWithZones.getToZoneId())) {
      throw new InvalidTripRequestException("User zones don't contain to or from location");
    }

    // Make sure request time makes sense (is essentially now or in the future -
    // i.e., not well in the past
    if (getPrimaryTimeConstraint(tripRequestWithZones)
        .isBefore(OffsetDateTime.now().minusSeconds(30))) {
      throw new InvalidTripRequestException("Request has unset time or is in the past");
    }

    return tripRequestWithZones;
  }

  public OffsetDateTime getPrimaryTimeConstraint(final TripRequest request) {
    return ObjectUtils.getOrDefault(
        ObjectUtils.getOrDefault(
            request.getPrimaryTimeConstraint(),
            new TimeConstraint().time(OffsetDateTime.MAX.minusYears(1))).getTime(),
        OffsetDateTime.MAX.minusYears(1));
  }

  private boolean isActive(final TripRequest request, final OffsetDateTime time) {
    return List
        .of(TripRequestStatus.NEW,
            TripRequestStatus.DRIVER_ASSIGNED,
            TripRequestStatus.DRIVER_EN_ROUTE,
            TripRequestStatus.DRIVER_ARRIVED,
            TripRequestStatus.TRIP_IN_PROGRESS,
            TripRequestStatus.CANCEL_BY_DRIVER_RIDER_LATE,
            TripRequestStatus.CANCEL_BY_DRIVER_RIDER_NOT_PRESENT)
        .contains(ObjectUtils.getOrDefault(request.getStatus(), TripRequestStatus.NEW)) && (
        (Math.abs(ChronoUnit.HOURS.between(getPrimaryTimeConstraint(request), time)) <= 2) || (
            (Math.abs(ChronoUnit.HOURS.between(
                ObjectUtils.getOrDefault(
                    request.getLastUpdated(),
                    OffsetDateTime.MAX.minusYears(1)),
                time)) <= 2) &&
                (request.getStatus() != TripRequestStatus.NEW)));
  }

  public TripRequest getActiveTripForRider(final UUID riderId, final OffsetDateTime now) {
    return StreamUtils.streamIterable(tripRepository.findByRiderId(riderId))
        .filter(tripRequest -> isActive(tripRequest, now))
        .min(Comparator.nullsLast(Comparator.comparing(this::getPrimaryTimeConstraint)))
        .orElse(null);
  }
}
