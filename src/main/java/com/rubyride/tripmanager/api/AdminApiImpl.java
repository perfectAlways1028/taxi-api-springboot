package com.rubyride.tripmanager.api;

import com.rubyride.tripmanager.utility.AdminUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AdminApiImpl {
  private final AdminUtils adminUtils;

  public AdminApiImpl(final AdminUtils adminUtils) {
    this.adminUtils = adminUtils;
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> archiveShiftsAndTrips(final OffsetDateTime time) {
    adminUtils.archiveShiftsAndTrips(time);

    return ResponseEntity.ok()
        .build();
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> reindexRepositories() {
    adminUtils.reindexRepositories();

    return ResponseEntity.ok()
        .build();
  }
}
