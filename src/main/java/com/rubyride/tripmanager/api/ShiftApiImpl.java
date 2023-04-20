package com.rubyride.tripmanager.api;

import com.rubyride.model.*;
import com.rubyride.tripmanager.event.ModifyShiftEvent;
import com.rubyride.tripmanager.event.NewShiftEvent;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.DriverRepository;
import com.rubyride.tripmanager.repository.mongo.ShiftArchiveRepository;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.repository.redis.ShiftRepository;
import com.rubyride.tripmanager.security.AccessControl;
import com.rubyride.tripmanager.utility.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShiftApiImpl {
  private final AccessControl accessControl;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final DriverRepository driverRepository;
  private final ShiftRepository shiftRepository;
  private final ShiftArchiveRepository shiftArchiveRepository;
  private final ZoneRepository zoneRepository;

  public ShiftApiImpl(final AccessControl accessControl, final ApplicationEventPublisher applicationEventPublisher, final DriverRepository driverRepository, final ShiftRepository shiftRepository, final ShiftArchiveRepository shiftArchiveRepository, final ZoneRepository zoneRepository, final AnalyticsUtils analyticsUtils) {
    this.accessControl = accessControl;
    this.applicationEventPublisher = applicationEventPublisher;
    this.driverRepository = driverRepository;
    this.shiftRepository = shiftRepository;
    this.shiftArchiveRepository = shiftArchiveRepository;
    this.zoneRepository = zoneRepository;
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> addEvent(final UUID shiftId, @Valid final Event event) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(SchedulingUtils.addOrUpdateEvent(shift, event, null));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, false));

          return ResponseEntity.created(URI.create("/v1/shifts/" + shift.getId().toString()))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> updateEvent(final UUID shiftId, @Valid final Event event) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(SchedulingUtils.addOrUpdateEvent(shift, event, null));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, false));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shift.getId().toString()))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> assignDriver(final UUID shiftId, @NotNull @Valid final UUID driverId) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(shift.driverId(driverId));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shift.getId().toString()))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shift.driverId)")
  public ResponseEntity<Shift> createShift(@Valid final Shift shift) {
    shift.setId(UUID.randomUUID());
    shift.setCreated(OffsetDateTime.now());
    shift.addEventsItem(new Event()
        .id(UUID.randomUUID())
        .action(EventAction.SHIFT_START));
    shift.addEventsItem(new Event()
        .id(UUID.randomUUID())
        .action(EventAction.SHIFT_END));

    shiftRepository.save(shift);

    applicationEventPublisher.publishEvent(new NewShiftEvent(shift));
    applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, true));

    return ResponseEntity.created(URI.create("/v1/shifts/" + shift.getId().toString()))
        .body(shift);
  }

  @PreAuthorize("@accessControl.canAccessShift(#shift.driverId)")
  public ResponseEntity<Shift> updateShift(@Valid final Shift shift) {
    return shiftRepository.findById(shift.getId())
        .map(existingShift -> {
          if (shift.getZoneId() != null) {
            existingShift.setZoneId(shift.getZoneId());
          }

          if (shift.getDriverId() != null) {
            existingShift.setDriverId(shift.getDriverId());
          }

          if (shift.getStartTime() != null) {
            existingShift.setStartTime(shift.getStartTime());
          }

          if (shift.getStartBuffer() != null) {
            existingShift.setStartBuffer(shift.getStartBuffer());
          }

          if (shift.getEndTime() != null) {
            existingShift.setEndTime(shift.getEndTime());
          }

          if (shift.getEndBuffer() != null) {
            existingShift.setEndBuffer(shift.getEndBuffer());
          }

          if (shift.getCreated() != null) {
            existingShift.setCreated(shift.getCreated());
          }

          if (shift.getCreatedBy() != null) {
            existingShift.setCreatedBy(shift.getCreatedBy());
          }

          if (shift.getTrips() != null) {
            existingShift.setTrips(shift.getTrips());
          }

          if (shift.getEvents() != null) {
            existingShift.setEvents(shift.getEvents());
          }

          if (shift.getActive() != null) {
            existingShift.setActive(shift.getActive());
          }

          shiftRepository.save(existingShift);
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + existingShift.getId().toString()))
              .body(existingShift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Void> deleteShift(final UUID shiftId) {
    shiftRepository.findById(shiftId)
        .ifPresent(shift -> {
          if (!ObjectUtils.getOrDefault(shift.getTrips(), Collections.emptyList()).isEmpty()) {
            throw new IllegalStateException("Cannot delete a shift with trips assigned to it.");
          }
          shiftRepository.deleteById(shiftId);
        });

    return ResponseEntity.noContent()
        .build();
  }

  @PreAuthorize("@accessControl.canAccessShifts()")
  public ResponseEntity<List<Shift>> getActiveShifts(final Boolean active) {
    return ResponseEntity.ok(shiftRepository.findByActive(active));
  }

  public ResponseEntity<String> getAnalytics(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final List<UUID> driverId, @Valid final List<UUID> zoneId) {
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .build();
  }

  private ZoneOffset getTimeZoneForDriver(final UUID driverId) {
    return driverId != null ? Optional.of(
        Optional.ofNullable(driverRepository.findByUserId(driverId)))
        .orElse(driverRepository.findById(driverId))
        .map(Driver::getHomeZone)
        .map(zoneRepository::findById)
        .flatMap(Function.identity())
        .map(Zone::getTimeZone)
        .map(ZoneOffset::ofHours)
        .orElse(ZoneOffset.UTC) :
        ZoneOffset.UTC;
  }

  public ResponseEntity<List<Shift>> getArchivedShifts(final LocalDate from, final LocalDate to, final UUID driverId) {
    final var dates = DateUtils.getMinMaxDates(from, to);
    final var timezone = getTimeZoneForDriver(driverId);

    return ResponseEntity.ok(StreamUtils.merge(driverId != null ?
            StreamUtils.safeStream(shiftRepository.findByDriverId(driverId)) :
            StreamUtils.streamIterable(shiftRepository.findAll()),
        driverId != null ?
            StreamUtils.safeStream(shiftArchiveRepository.findByDriverId(driverId)) :
            StreamUtils.streamIterable(shiftArchiveRepository.findAll()))
        .filter(shift ->
            ObjectUtils.getOrDefault(shift.getStartTime(), OffsetDateTime.now())
                .atZoneSameInstant(timezone).toLocalDate()
                .compareTo(dates.getSecond()) <= 0 &&
                ObjectUtils.getOrDefault(shift.getEndTime(), OffsetDateTime.now())
                    .atZoneSameInstant(timezone).toLocalDate()
                    .compareTo(dates.getFirst()) >= 0)
        .filter(shift -> accessControl.canAccessShift(shift.getId()))
        .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> getShiftById(final UUID shiftId) {
    return ResponseEntity.of(Optional.ofNullable(shiftRepository.findById(shiftId)
        .orElse(shiftArchiveRepository.findById(shiftId)
            .orElse(null))));
  }

  public ResponseEntity<List<Shift>> getShiftsForDriver(final UUID driverId, @Valid final Boolean active, @Valid final LocalDate startDate, @Valid final LocalDate endDate) {
    final var dates = DateUtils.getMinMaxDates(startDate, endDate);
    final var timezone = getTimeZoneForDriver(driverId);

    return ResponseEntity.ok(StreamUtils.streamIterable(shiftRepository.findByDriverId(driverId))
        .filter(shift -> active == null || active.equals(shift.getActive()))
        .filter(shift ->
            ObjectUtils.getOrDefault(shift.getStartTime(), OffsetDateTime.now())
                .atZoneSameInstant(timezone).toLocalDate()
                .compareTo(dates.getSecond()) <= 0 &&
                ObjectUtils.getOrDefault(shift.getEndTime(), OffsetDateTime.now())
                    .atZoneSameInstant(timezone).toLocalDate()
                    .compareTo(dates.getFirst()) >= 0)
        .filter(shift -> accessControl.canAccessShift(shift.getId()))
        .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canAccessShifts()")
  public ResponseEntity<List<Shift>> getShiftsForZone(final UUID zoneId, final LocalDate date) {
    return zoneRepository.findById(zoneId)
        .map(zone -> ResponseEntity.ok(StreamUtils.streamIterable(shiftRepository.findByZoneId(zoneId))
            .filter(shift -> date == null || (
                ObjectUtils.getOrDefault(shift.getStartTime(), OffsetDateTime.now())
                    .atZoneSameInstant(ZoneOffset.ofHours(zone.getTimeZone())).toLocalDate()
                    .compareTo(date) <= 0 &&
                    ObjectUtils.getOrDefault(shift.getEndTime(), OffsetDateTime.now())
                        .atZoneSameInstant(ZoneOffset.ofHours(zone.getTimeZone())).toLocalDate()
                        .compareTo(date) >= 0))
            .collect(Collectors.toList())))
        .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> moveEvent(final UUID shiftId, final UUID eventId, @NotNull @Valid final UUID anchorEventId) {
    if (anchorEventId.equals(eventId)) {
      return ResponseEntity.badRequest()
          .build();
    }

    return shiftRepository.findById(shiftId)
        .map(shift -> StreamUtils.safeStream(shift.getEvents())
            .filter(event -> event.getId().equals(anchorEventId))
            .findFirst()
            .map(anchorEvent -> StreamUtils.safeStream(shift.getEvents())
                .filter(event -> event.getId().equals(eventId))
                .findFirst()
                .map(event -> {
                  SchedulingUtils.addOrUpdateEvent(shift, event, anchorEventId);

                  shiftRepository.save(shift);
                  applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, false));

                  return ResponseEntity.ok()
                      .location(URI.create("/v1/shifts/" + shift.getId().toString()))
                      .body(shift);
                })
                .orElseThrow(() -> new EntityNotFoundException("Event not found")))
            .orElseThrow(() -> new EntityNotFoundException("Anchor event not found")))
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> removeDriver(final UUID shiftId) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(shift.driverId(null));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shiftId))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> removeEvent(final UUID shiftId, final UUID eventId) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          final var events = shift.getEvents();

          shiftRepository.save(shift.events(events.stream()
              .filter(event -> !eventId.equals(event.getId()))
              .collect(Collectors.toList())));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, false));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shiftId))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> setEndTime(final UUID shiftId, @NotNull @Valid final OffsetDateTime endTime) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(shift.endTime(endTime));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, EventAction.SHIFT_END, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shift.getId().toString()))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> setShiftActive(final UUID shiftId, final Boolean active) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(shift.active(active));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, null, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shiftId))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }

  @PreAuthorize("@accessControl.canAccessShift(#shiftId)")
  public ResponseEntity<Shift> setStartTime(final UUID shiftId, @NotNull @Valid final OffsetDateTime startTime) {
    return shiftRepository.findById(shiftId)
        .map(shift -> {
          shiftRepository.save(shift.startTime(startTime));
          applicationEventPublisher.publishEvent(new ModifyShiftEvent(shift, null, EventAction.SHIFT_START, true));

          return ResponseEntity.ok()
              .location(URI.create("/v1/shifts/" + shiftId))
              .body(shift);
        })
        .orElseThrow(() -> new EntityNotFoundException("Shift not found"));
  }
}
