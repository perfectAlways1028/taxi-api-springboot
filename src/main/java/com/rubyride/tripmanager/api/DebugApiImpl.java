package com.rubyride.tripmanager.api;

import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.service.NotifyService;
import com.rubyride.tripmanager.service.TwilioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class DebugApiImpl {
  private final UserRepository userRepository;
  private final NotifyService notifyService;

  public DebugApiImpl(final UserRepository userRepository, final NotifyService notifyService) {
    this.userRepository = userRepository;
    this.notifyService = notifyService;
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<Void> sendTestNotification(final UUID userId) {
    final var result = userRepository.findById(userId)
        .map(user -> notifyService.pushToSubscriptionAndSendNotification(userId, true, false, NotifyService.Topic.TRIP, UUID.randomUUID().toString(), "test", "test", OffsetDateTime.now()))
        .orElseThrow(() -> new EntityNotFoundException("User not found"));

    return ResponseEntity.ok()
        .build();
  }
}
