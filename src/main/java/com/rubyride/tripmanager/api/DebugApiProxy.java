package com.rubyride.tripmanager.api;

import com.rubyride.api.DebugApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class DebugApiProxy implements DebugApi {
  private final DebugApiImpl debugApi;

  public DebugApiProxy(final DebugApiImpl debugApi) {
    this.debugApi = debugApi;
  }

  @Override
  public ResponseEntity<Void> sentTestNotification(final UUID userId) {
    return debugApi.sendTestNotification(userId);
  }
}
