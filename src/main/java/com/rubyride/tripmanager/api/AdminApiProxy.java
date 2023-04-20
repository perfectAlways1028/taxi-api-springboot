package com.rubyride.tripmanager.api;

import com.rubyride.api.AdminApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.OffsetDateTime;

@RestController
public class AdminApiProxy implements AdminApi {
  private final AdminApiImpl adminApi;

  public AdminApiProxy(final AdminApiImpl adminApi) {
    this.adminApi = adminApi;
  }

  @Override
  public ResponseEntity<Void> archiveShiftsAndTrips(@Valid final OffsetDateTime time) {
    return adminApi.archiveShiftsAndTrips(time);
  }

  @Override
  public ResponseEntity<Void> reindexRepositories() {
    return adminApi.reindexRepositories();
  }
}
