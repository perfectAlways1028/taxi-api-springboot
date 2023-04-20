package com.rubyride.tripmanager.service;

import com.rubyride.model.LatitudeLongitude;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.utility.StreamUtils;
import net.iakovlev.timeshape.TimeZoneEngine;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class TimeZoneService {
  private final ZoneRepository zoneRepository;
  private TimeZoneEngine engine;

  public TimeZoneService(final ZoneRepository zoneRepository) {
    this.zoneRepository = zoneRepository;
  }

  @PostConstruct
  public void initialize() {
    engine = TimeZoneEngine.initialize();
  }

  public Optional<ZoneId> getTimeZone(final LatitudeLongitude location) {
    return Optional.ofNullable(location)
        .flatMap(loc -> engine.query(loc.getLatitude(), loc.getLongitude()));
  }

  @Scheduled(cron = "0 0 * * * ?")
  public void updateTimezonesForZones() {
    final var now = Instant.now();

    zoneRepository.findAll()
        .forEach(zone -> {
          final var currentTimezone = zone.getTimeZone();

          getTimeZone(zone.getMapCenter())
              .map(ZoneId::getRules)
              .map(zoneRules -> zoneRules.getOffset(now))
              .map(ZoneOffset::getTotalSeconds)
              .map(seconds -> seconds / (60 * 60)) // convert to hours
              .filter(StreamUtils.not(currentTimezone::equals))
              .map(zone::timeZone)
              .ifPresent(zoneRepository::save);
        });
  }
}
