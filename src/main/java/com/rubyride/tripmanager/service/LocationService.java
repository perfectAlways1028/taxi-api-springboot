package com.rubyride.tripmanager.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rubyride.model.LatitudeLongitude;
import com.rubyride.tripmanager.event.DriverLocationSetEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class LocationService {
  private final ApplicationEventPublisher applicationEventPublisher;

  private final Cache<UUID, LatitudeLongitude> driverLocations = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(30L))
      .build();

  public LocationService(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public Optional<LatitudeLongitude> getLocation(final UUID driverId) {
    return Optional.ofNullable(driverLocations.getIfPresent(driverId));
  }

  public void flushLocation(final UUID driverId) {
    driverLocations.invalidate(driverId);
  }

  public void setLocation(@DestinationVariable("driverId") final UUID driverId, @Payload final LatitudeLongitude location) {
    driverLocations.put(driverId, location);
    applicationEventPublisher.publishEvent(new DriverLocationSetEvent(driverId, location));
  }
}
