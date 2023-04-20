package com.rubyride.tripmanager.api;

import com.rubyride.model.InlineResponse200;
import com.rubyride.model.TripRequest;
import com.rubyride.model.TripRequestScheduleType;
import com.rubyride.model.TripRequestStatus;
import com.rubyride.tripmanager.repository.mongo.TripArchiveRepository;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.utility.DateUtils;
import com.rubyride.tripmanager.utility.StreamUtils;
import com.rubyride.tripmanager.utility.TripUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@PreAuthorize("hasAnyAuthority('ADMIN,DISPATCHER')")
public class AnalyticsApiImpl {
  private final TripRepository tripRepository;
  private final TripArchiveRepository tripArchiveRepository;
  private final TripUtils tripUtils;
  private final ZoneRepository zoneRepository;

  public AnalyticsApiImpl(final TripRepository tripRepository, final TripArchiveRepository tripArchiveRepository, final TripUtils tripUtils, final ZoneRepository zoneRepository) {
    this.tripRepository = tripRepository;
    this.tripArchiveRepository = tripArchiveRepository;
    this.tripUtils = tripUtils;
    this.zoneRepository = zoneRepository;
  }

  private Stream<TripRequest> getTrips(final LocalDate from, final LocalDate to, final UUID zoneId) {
    final Stream<TripRequest> tripRequests;

    final var dates = DateUtils.getMinMaxDates(from, to);

    if (zoneId != null) {
      tripRequests = zoneRepository.findById(zoneId)
          .map(zone -> StreamUtils.merge(tripRepository.findByFromZoneId(zone.getId()).stream(),
              tripRepository.findByToZoneId(zone.getId()).stream(),
              tripArchiveRepository.findByFromZoneId(zone.getId()).stream(),
              tripArchiveRepository.findByToZoneId(zone.getId()).stream())
              .unordered()
              .distinct()
              .filter(request -> {
                final var requestDate = tripUtils.getPrimaryTimeConstraint(request).atZoneSameInstant(ZoneOffset.ofHours(zone.getTimeZone())).toLocalDate();
                return dates.getFirst().compareTo(requestDate) <= 0 &&
                    dates.getSecond().compareTo(requestDate) > 0;
              }))
          .orElse(Stream.empty());
    } else {
      tripRequests = StreamUtils.merge(StreamUtils.streamIterable(tripRepository.findAll()),
          StreamUtils.streamIterable(tripArchiveRepository.findAll()))
          .filter(request -> {
            final var requestDate = tripUtils.getPrimaryTimeConstraint(request).toLocalDate();
            return dates.getFirst().compareTo(requestDate) <= 0 &&
                dates.getSecond().compareTo(requestDate) > 0;
          });
    }

    return tripRequests;
  }

  public ResponseEntity<InlineResponse200> getAggregatedTripCounts(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final UUID zoneId) {
    final var groupedTripRequests = getTrips(from, to, zoneId)
        .collect(Collectors.groupingBy(TripRequest::getStatus,
            Collectors.counting()));

    return ResponseEntity.ok(new InlineResponse200()
        .notStarted(groupedTripRequests.getOrDefault(TripRequestStatus.NEW, 0L) +
            groupedTripRequests.getOrDefault(TripRequestStatus.DRIVER_ASSIGNED, 0L))
        .enRoute(groupedTripRequests.getOrDefault(TripRequestStatus.DRIVER_EN_ROUTE, 0L))
        .active(groupedTripRequests.getOrDefault(TripRequestStatus.DRIVER_ARRIVED, 0L) +
            groupedTripRequests.getOrDefault(TripRequestStatus.TRIP_IN_PROGRESS, 0L))
        .complete(groupedTripRequests.getOrDefault(TripRequestStatus.TRIP_COMPLETE, 0L))
        .cancel(groupedTripRequests.getOrDefault(TripRequestStatus.CANCEL_BY_RIDER, 0L) +
            groupedTripRequests.getOrDefault(TripRequestStatus.CANCEL_BY_DRIVER_RIDER_LATE, 0L) +
            groupedTripRequests.getOrDefault(TripRequestStatus.CANCEL_BY_DRIVER_RIDER_NOT_PRESENT, 0L)));
  }

  public ResponseEntity<Map<String, Long>> getTripCounts(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate
      to, @Valid final UUID zoneId, @Valid final TripRequestScheduleType scheduleType) {
    return ResponseEntity.ok(getTrips(from, to, zoneId)
        .filter(request -> scheduleType == null || scheduleType.equals(request.getScheduleType()))
        .collect(Collectors.groupingBy(tripRequest -> tripUtils.getPrimaryTimeConstraint(tripRequest).toLocalDate().toString(),
            TreeMap::new,
            Collectors.counting())));
  }
}
