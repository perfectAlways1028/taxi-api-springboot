package com.rubyride.tripmanager.service;

import com.rubyride.tripmanager.utility.AdminUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Component
public class ArchiveService {
  private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);

  private final AdminUtils adminUtils;

  public ArchiveService(final AdminUtils adminUtils) {
    this.adminUtils = adminUtils;
  }

  @Scheduled(cron = "0 0 5 1/1 * ?")
  public void archiveShiftsAndTrips() {
    final var thresholdTime = OffsetDateTime.now()
        .minus(1, ChronoUnit.WEEKS);

    log.info("Archiving shifts and trip requests older than " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(thresholdTime));

    adminUtils.archiveShiftsAndTrips(thresholdTime);
  }
}
