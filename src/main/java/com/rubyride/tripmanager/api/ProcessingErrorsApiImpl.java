package com.rubyride.tripmanager.api;

import com.rubyride.model.TripSchedulingException;
import com.rubyride.tripmanager.repository.mongo.TripArchiveRepository;
import com.rubyride.tripmanager.repository.mongo.TripSchedulingExceptionRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.utility.DateUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class ProcessingErrorsApiImpl {
  private final TripSchedulingExceptionRepository tripSchedulingExceptionRepository;
  private final TripRepository tripRepository;
  private final TripArchiveRepository tripArchiveRepository;

  public ProcessingErrorsApiImpl(final TripSchedulingExceptionRepository tripSchedulingExceptionRepository, final TripRepository tripRepository, final TripArchiveRepository tripArchiveRepository) {
    this.tripSchedulingExceptionRepository = tripSchedulingExceptionRepository;
    this.tripRepository = tripRepository;
    this.tripArchiveRepository = tripArchiveRepository;
  }

  @MessageMapping("/processingExceptions")
  @SendTo("/topic/processingExceptions")
  public TripSchedulingException addTripSchedulingException(final TripSchedulingException tripSchedulingException) {
    tripSchedulingExceptionRepository.save(tripSchedulingException
        .id(UUID.randomUUID())
        .time(OffsetDateTime.now()));

    return tripSchedulingException;
  }

  @PreAuthorize("@accessControl.canAccessTripSchedulingExceptions()")
  public ResponseEntity<List<TripSchedulingException>> getTripProcessingErrors(final UUID zoneId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
    final var dates = DateUtils.getMinMaxDates(startDate, endDate);

    final var tripSchedulingExceptions = tripSchedulingExceptionRepository.findByTimeBetween(dates.getFirst(), dates.getSecond());

    return ResponseEntity.ok(zoneId == null ?
        tripSchedulingExceptions :
        tripSchedulingExceptions.stream()
            .filter(tripSchedulingException ->
                Stream.of(tripRepository.findById(tripSchedulingException.getTripRequestId()),
                    tripArchiveRepository.findById(tripSchedulingException.getTripRequestId()))
                    .flatMap(Optional::stream)
                    .findAny()
                    .map(tripRequest -> zoneId.equals(tripRequest.getFromZoneId()) || zoneId.equals(tripRequest.getToZoneId()))
                    .orElse(false))
            .collect(Collectors.toList()));
  }
}
