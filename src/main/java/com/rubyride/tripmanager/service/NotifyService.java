package com.rubyride.tripmanager.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class NotifyService {
  private static final Logger log = LoggerFactory.getLogger(NotifyService.class);

  private final TwilioService twilioService;
  private final SimpMessageSendingOperations messageService;
  private final ObjectMapper objectMapper;

  public NotifyService(final TwilioService twilioService, final SimpMessageSendingOperations messageService, final ObjectMapper objectMapper) {
    this.twilioService = twilioService;
    this.messageService = messageService;
    this.objectMapper = objectMapper;
  }

  public <T> boolean pushToSubscriptionAndSendNotification(final UUID userId, final boolean shouldPushNotification, final boolean dataOnly, final Topic topic, final String dataId, final String title, final String message, final T data) {
    final var destinationTopic = topic.topic + (dataId != null ?
        "/" + dataId :
        "");

    messageService.convertAndSend("/topic/" + destinationTopic, data);
    log.info("Message delivered to WS endpoint " + destinationTopic);

    if (userId != null && shouldPushNotification) {
      try {
        final var notification = twilioService.notify(
            destinationTopic,
            userId,
            dataOnly ?
                null :
                title,
            dataOnly ?
                null :
                message,
            Map.of(topic.topic, objectMapper.writeValueAsString(data)));

        log.info("Sending push notification [" + notification.getSid() + "]" +
            " to " + userId + " with topic " + topic.topic);
      } catch (final Exception e) {
        log.error("Exception caught", e);
      }
    }

    return true;
  }

  public void sendSMS(final String phoneNumber, final String message) {
    try {
      final var sms = twilioService.sendSMS(phoneNumber, message);
      log.debug("Sending SMS " + sms.getSid() + " to " + phoneNumber);
    } catch (final Exception e) {
      log.error("Exception caught", e);
    }
  }

  public enum Topic {
    TRIP("trip"),
    TRIPS("trips"),
    SHIFT("shift"),
    SHIFTS("shifts"),
    DRIVER_LOCATION("driverLocation"),
    DRIVER_LOCATIONS("driverLocations");

    private final String topic;

    Topic(final String topic) {
      this.topic = topic;
    }
  }
}
