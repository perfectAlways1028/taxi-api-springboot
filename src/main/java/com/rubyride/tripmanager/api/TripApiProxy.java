package com.rubyride.tripmanager.api;

import com.rubyride.api.TripApi;
import com.rubyride.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class TripApiProxy implements TripApi {
  private final TripApiImpl tripApiImpl;

  @Autowired
  public TripApiProxy(final TripApiImpl tripApiImpl) {
    this.tripApiImpl = tripApiImpl;
  }

  @Override
  public ResponseEntity<TripRequest> assignTripToShift(final UUID tripId, final UUID shiftId, @Min(0) @Valid final Integer position) {
    return tripApiImpl.assignTripToShift(tripId, shiftId, position);
  }

  @Override
  public ResponseEntity<TripRequest> cancelTripById(final UUID tripId, final TripRequestStatus reason) {
    return tripApiImpl.cancelTripById(tripId, reason);
  }

  @Override
  public ResponseEntity<Void> deleteTrip(final UUID tripId) {
    return tripApiImpl.deleteTrip(tripId);
  }

  @Override
  public ResponseEntity<TripRequestWithLocationsAndDriverDetails> getActiveTrip(final UUID riderId) {
    return tripApiImpl.getActiveTrip(riderId);
  }

  @Override
  public ResponseEntity<List<TripRequest>> getAllTrips() {
    return tripApiImpl.getAllTrips();
  }

  @Override
  public ResponseEntity<List<TripRequest>> getArchivedTrips(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final UUID zoneId, @Valid final UUID riderId) {
    return tripApiImpl.getArchivedTrips(from, to, zoneId, riderId);
  }

  @Override
  public ResponseEntity<List<TripRequest>> getTrip(final List<UUID> tripId) {
    return tripApiImpl.getTrip(tripId);
  }

  @Override
  public ResponseEntity<DriverDetails> getTripDriverDetails(final UUID tripId) {
    return tripApiImpl.getTripDriverDetails(tripId);
  }

  @Override
  public ResponseEntity<List<TripRequest>> getTripsForDriver(final UUID driverId) {
    return tripApiImpl.getTripsForDriver(driverId);
  }

  @Override
  public ResponseEntity<List<TripRequest>> getTripsForZone(final UUID zoneId, @Valid final LocalDate date) {
    return tripApiImpl.getTripsForZone(zoneId, date);
  }

  @Override
  public ResponseEntity<List<TripRequestWithLocationsAndDriverDetails>> getUpcomingTrips(final UUID riderId) {
    return tripApiImpl.getUpcomingTrips(riderId);
  }

  @Override
  public ResponseEntity<TripRequest> requestTrip(@Valid final TripRequest tripRequest) {
    return tripApiImpl.requestTrip(tripRequest);
  }

  @Override
  public ResponseEntity<TripRequest> tripDropOffComplete(final UUID tripId, @Valid final LatitudeLongitude latitudeLongitude) {
    return tripApiImpl.tripDropOffComplete(tripId, latitudeLongitude);
  }

  @Override
  public ResponseEntity<TripRequest> tripDropffArrived(final UUID tripId, @Valid final LatitudeLongitude latitudeLongitude) {
    return tripApiImpl.tripDropffArrived(tripId, latitudeLongitude);
  }

  @Override
  public ResponseEntity<TripRequest> tripEnroute(final UUID tripId, @Valid final LatitudeLongitude latitudeLongitude) {
    return tripApiImpl.tripEnroute(tripId, latitudeLongitude);
  }

  @Override
  public ResponseEntity<TripRequest> tripPickUpComplete(final UUID tripId, @Valid final LatitudeLongitude latitudeLongitude) {
    return tripApiImpl.tripPickUpComplete(tripId, latitudeLongitude);
  }

  @Override
  public ResponseEntity<TripRequest> tripPickupArrived(final UUID tripId, @Valid final LatitudeLongitude latitudeLongitude) {
    return tripApiImpl.tripPickupArrived(tripId, latitudeLongitude);
  }

  @Override
  public ResponseEntity<TripRequest> updateTrip(@Valid final TripRequest tripRequest) {
    return tripApiImpl.updateTrip(tripRequest);
  }

  @Override
  public ResponseEntity<TripRequest> setNeedsAssigned(final UUID tripId) {
    return tripApiImpl.setTripNeedsAssigned(tripId);
  }
}
