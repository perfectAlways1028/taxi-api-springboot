package com.rubyride.tripmanager.api;

import com.rubyride.model.LatitudeLongitude;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.DriverRepository;
import com.rubyride.tripmanager.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.UUID;

@Controller
public class LocationApiImpl {
  private static final Logger log = LoggerFactory.getLogger(LocationApiImpl.class);

  private final DriverRepository driverRepository;
  private final LocationService locationService;

  @Autowired
  public LocationApiImpl(final DriverRepository driverRepository, final LocationService locationService) {
    this.driverRepository = driverRepository;
    this.locationService = locationService;
  }

  @MessageMapping("/driverLocation/{driverId}/setLocation")
  @PreAuthorize("@accessControl.canAccessLocation(#driverId)")
  public void updateLocation(@DestinationVariable("driverId") final UUID driverId, @Payload final LatitudeLongitude location) {
    Optional.ofNullable(driverRepository.findByUserId(driverId))
        .ifPresentOrElse(driver -> locationService.setLocation(driverId, location),
            () -> {
              throw new EntityNotFoundException("Driver not found");
            });
  }

  @PreAuthorize("@accessControl.canAccessLocation(#driverId)")
  public ResponseEntity<LatitudeLongitude> getLocation(final UUID driverId) {
    return Optional.ofNullable(driverRepository.findByUserId(driverId))
        .map(driver -> locationService.getLocation(driver.getId()))
        .map(ResponseEntity::of)
        .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
  }
}
