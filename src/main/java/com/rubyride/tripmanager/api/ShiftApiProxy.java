package com.rubyride.tripmanager.api;

import com.rubyride.api.ShiftApi;
import com.rubyride.model.Event;
import com.rubyride.model.Shift;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class ShiftApiProxy implements ShiftApi {
  private final ShiftApiImpl shiftApiImpl;

  public ShiftApiProxy(final ShiftApiImpl shiftApiImpl) {
    this.shiftApiImpl = shiftApiImpl;
  }

  @Override
  public ResponseEntity<Shift> addEvent(final UUID shiftId, @Valid final Event event) {
    return shiftApiImpl.addEvent(shiftId, event);
  }

  @Override
  public ResponseEntity<Shift> assignDriver(final UUID shiftId, @NotNull @Valid final UUID driverId) {
    return shiftApiImpl.assignDriver(shiftId, driverId);
  }

  @Override
  public ResponseEntity<Shift> createShift(@Valid final Shift shift) {
    return shiftApiImpl.createShift(shift);
  }

  @Override
  public ResponseEntity<Void> deleteShift(final UUID shiftId) {
    return shiftApiImpl.deleteShift(shiftId);
  }

  @Override
  public ResponseEntity<List<Shift>> getActiveShifts(final Boolean active) {
    return shiftApiImpl.getActiveShifts(active);
  }

  @Override
  public ResponseEntity<String> getAnalytics(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final List<UUID> driverId, @Valid final List<UUID> zoneId) {
    return shiftApiImpl.getAnalytics(from, to, driverId, zoneId);
  }

  @Override
  public ResponseEntity<List<Shift>> getArchivedShifts(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final UUID driverId) {
    return shiftApiImpl.getArchivedShifts(from, to, driverId);
  }

  @Override
  public ResponseEntity<Shift> getShiftById(final UUID shiftId) {
    return shiftApiImpl.getShiftById(shiftId);
  }

  @Override
  public ResponseEntity<List<Shift>> getShiftsForDriver(final UUID driverId, @Valid final Boolean active, @Valid final LocalDate startDate, @Valid final LocalDate endDate) {
    return shiftApiImpl.getShiftsForDriver(driverId, active, startDate, endDate);
  }

  @Override
  public ResponseEntity<List<Shift>> getShiftsForZone(final UUID zoneId, @Valid final LocalDate date) {
    return shiftApiImpl.getShiftsForZone(zoneId, date);
  }

  @Override
  public ResponseEntity<Shift> moveEvent(final UUID shiftId, final UUID eventId, @NotNull @Valid final UUID anchorEventId) {
    return shiftApiImpl.moveEvent(shiftId, eventId, anchorEventId);
  }

  @Override
  public ResponseEntity<Shift> removeDriver(final UUID shiftId) {
    return shiftApiImpl.removeDriver(shiftId);
  }

  @Override
  public ResponseEntity<Shift> removeEvent(final UUID shiftId, final UUID eventId) {
    return shiftApiImpl.removeEvent(shiftId, eventId);
  }

  @Override
  public ResponseEntity<Shift> setEndTime(final UUID shiftId, @NotNull @Valid final OffsetDateTime endTime) {
    return shiftApiImpl.setEndTime(shiftId, endTime);
  }

  @Override
  public ResponseEntity<Shift> setShiftActive(final UUID shiftId, final Boolean active) {
    return shiftApiImpl.setShiftActive(shiftId, active);
  }

  @Override
  public ResponseEntity<Shift> setStartTime(final UUID shiftId, @NotNull @Valid final OffsetDateTime startTime) {
    return shiftApiImpl.setStartTime(shiftId, startTime);
  }

  @Override
  public ResponseEntity<Shift> updateEvent(final UUID shiftId, @Valid final Event event) {
    return shiftApiImpl.updateEvent(shiftId, event);
  }

  @Override
  public ResponseEntity<Shift> updateShift(@Valid final Shift shift) {
    return shiftApiImpl.updateShift(shift);
  }
}
