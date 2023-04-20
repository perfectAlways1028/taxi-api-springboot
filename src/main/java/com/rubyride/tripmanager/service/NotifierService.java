package com.rubyride.tripmanager.service;

import com.rubyride.model.*;
import com.rubyride.tripmanager.event.*;
import com.rubyride.tripmanager.model.TripRequestIdAndStatus;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.utility.ObjectUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class NotifierService {
  private final UserRepository userRepository;
  private final PlaceRepository placeRepository;
  private final ZoneRepository zoneRepository;
  private final MessageSource messageSource;
  private final NotifyService notifyService;

  public NotifierService(final UserRepository userRepository, final PlaceRepository placeRepository, final ZoneRepository zoneRepository, final MessageSource messageSource, final NotifyService notifyService) {
    this.userRepository = userRepository;
    this.placeRepository = placeRepository;
    this.zoneRepository = zoneRepository;
    this.messageSource = messageSource;
    this.notifyService = notifyService;
  }

  private Object[] getMessageArguments(final TripRequest tripRequest, final User user, final Locale locale) {
    final var fromTimezone = Optional.ofNullable(tripRequest)
        .flatMap(trip -> zoneRepository.findById(trip.getFromZoneId()))
        .map(Zone::getTimeZone)
        .map(ZoneOffset::ofHours)
        .orElse(ZoneOffset.UTC);

    final var currentTime = OffsetDateTime.now()
        .atZoneSameInstant(fromTimezone)
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale));

    final var fromAddress = Optional.ofNullable(tripRequest)
        .flatMap(trip -> placeRepository.findById(trip.getFromLocationId())
            .map(place -> place.getAddress().getStreet()))
        .orElse("");

    final var toAddress = Optional.ofNullable(tripRequest)
        .flatMap(trip -> placeRepository.findById(trip.getToLocationId())
            .map(place -> place.getAddress().getStreet()))
        .orElse("");

    final var pickupTime = Optional.ofNullable(tripRequest)
        .flatMap(trip -> Optional.ofNullable(trip.getPrimaryTimeConstraint()))
        .map(TimeConstraint::getTime)
        .map(time -> time.atZoneSameInstant(fromTimezone))
        .map(time -> time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(locale)))
        .orElse("");

    final var pickupDate = Optional.ofNullable(tripRequest)
        .flatMap(trip -> Optional.ofNullable(trip.getPrimaryTimeConstraint()))
        .map(TimeConstraint::getTime)
        .map(time -> time.atZoneSameInstant(fromTimezone))
        .map(time -> time.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)))
        .orElse("");

    return new Object[]{
        user.getFirstName(), // 0 - user's name
        currentTime, // 1 - current time
        fromAddress, // 2 - from address
        toAddress, // 3 - to address
        pickupTime, // 4 - pickup time
        pickupDate // 5 - pickup date
    };
  }

  @EventListener(NewTripRequestEvent.class)
  public void notifyTripRequests(final NewTripRequestEvent event) {
    CompletableFuture.runAsync(() ->
        notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.TRIPS, null, null, null, event.getTripRequest().getId()));
  }

  @EventListener(NewShiftEvent.class)
  public void notifyShifts(final NewShiftEvent event) {
    CompletableFuture.runAsync(() ->
        notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.SHIFTS, null, null, null, event.getShift().getId()));
  }

  @EventListener(ModifyTripRequestEvent.class)
  public void notifyTripRequest(final ModifyTripRequestEvent event) {
    CompletableFuture.runAsync(() -> {
      final var tripRequest = event.getTripRequest();
      final var eventAction = event.getEventAction();

      userRepository.findById(tripRequest.getRiderId())
          .ifPresent(user -> {
            final var locale = Locale.forLanguageTag(ObjectUtils.getOrDefault(user.getLocale(), "en-US"));
            final var notificationType = ObjectUtils.getOrDefault(user.getNotificationType(), NotificationType.SMS);
            final var arguments = getMessageArguments(tripRequest, user, locale);

            String title = null;
            String message = null;

            try {
              title = messageSource.getMessage(
                  ObjectUtils.getOrDefault(tripRequest.getTripRequestType(), TripRequestType.PASSENGER).getValue() +
                      "." + tripRequest.getStatus().getValue() +
                      (eventAction != null ?
                          "." + eventAction.getValue() :
                          "")
                      + ".TITLE",
                  arguments,
                  locale);
              message = messageSource.getMessage(
                  ObjectUtils.getOrDefault(tripRequest.getTripRequestType(), TripRequestType.PASSENGER).getValue() +
                      "." + tripRequest.getStatus().getValue() +
                      (eventAction != null ?
                          "." + eventAction.getValue() :
                          "")
                      + "." + notificationType.getValue(),
                  arguments,
                  locale);

              final var finalMessage = message;

              Optional.ofNullable(user.getPrimaryPhone())
                  .filter(phoneNumber -> !phoneNumber.isEmpty() &&
                      notificationType == NotificationType.SMS || tripRequest.getTripRequestType() == TripRequestType.COURIER)
                  .ifPresent(phoneNumber -> notifyService.sendSMS(phoneNumber, finalMessage));
            } catch (final NoSuchMessageException e) {
              // no-op
            }

            // Send data notification for trip request to user
            notifyService.pushToSubscriptionAndSendNotification(
                user.getId(),
                notificationType == NotificationType.PUSH,
                false,
                NotifyService.Topic.TRIP,
                tripRequest.getId().toString(),
                title,
                message,
                new TripRequestIdAndStatus(tripRequest.getId(), tripRequest.getStatus()));
          });

      // Also notify TRIPS feed of modified trip request
      notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.TRIPS, null, null, null, event.getTripRequest().getId());
    });
  }

  @EventListener(ModifyShiftEvent.class)
  public void notifyShift(final ModifyShiftEvent event) {
    CompletableFuture.runAsync(() -> {
      final var shift = event.getShift();
      final var tripRequest = event.getTripRequest();
      final var eventAction = event.getEventAction();
      final var dataOnly = event.isDataOnly();

      if (shift.getDriverId() == null) {
        return;
      }

      userRepository.findById(shift.getDriverId())
          .ifPresent(user -> {
            final var locale = Locale.forLanguageTag(ObjectUtils.getOrDefault(user.getLocale(), "en-US"));
            final var notificationType = ObjectUtils.getOrDefault(user.getNotificationType(), NotificationType.SMS);
            final var arguments = getMessageArguments(tripRequest, user, locale);

            if (tripRequest != null) {
              try {
                final String tripStatus;

                if (tripRequest.getStatus() == TripRequestStatus.DRIVER_ASSIGNED &&
                    !shift.getId().equals(tripRequest.getShiftId())) {
                  tripStatus = "DRIVER_UNASSIGNED";
                } else {
                  tripStatus = tripRequest.getStatus().getValue();
                }

                final var title = messageSource.getMessage(
                    "SHIFT." +
                        ObjectUtils.getOrDefault(tripRequest.getTripRequestType(), TripRequestType.PASSENGER).getValue() +
                        "." + tripStatus +
                        (eventAction != null ?
                            "." + eventAction.getValue() :
                            "")
                        + ".TITLE",
                    arguments,
                    locale);
                final var message = messageSource.getMessage(
                    "SHIFT." +
                        ObjectUtils.getOrDefault(tripRequest.getTripRequestType(), TripRequestType.PASSENGER).getValue() +
                        "." + tripStatus +
                        (eventAction != null ?
                            "." + eventAction.getValue() :
                            "")
                        + "." + notificationType.getValue(),
                    arguments,
                    locale);

                final var phoneNumber = ObjectUtils.getOrDefault(user.getPrimaryPhone(), "");

                if (notificationType == NotificationType.SMS && !phoneNumber.isEmpty()) {
                  notifyService.sendSMS(phoneNumber, message);
                }

                // Send data notification for shift to user
                notifyService.pushToSubscriptionAndSendNotification(
                    user.getId(),
                    notificationType == NotificationType.PUSH,
                    dataOnly,
                    NotifyService.Topic.SHIFT,
                    shift.getId().toString(),
                    title,
                    message,
                    shift.getId());
              } catch (final NoSuchMessageException e) {
                // no-op
              }
            } else {
              try {
                final var title = messageSource.getMessage("SHIFT.TITLE",
                    arguments,
                    locale);
                final var message = messageSource.getMessage("SHIFT"
                        + "." + notificationType.getValue(),
                    arguments,
                    locale);

                // Send WS update for shift to user
                notifyService.pushToSubscriptionAndSendNotification(
                    user.getId(),
                    notificationType == NotificationType.PUSH,
                    dataOnly,
                    NotifyService.Topic.SHIFT,
                    shift.getId().toString(),
                    title,
                    message,
                    shift.getId());
              } catch (final NoSuchMessageException e) {
                // no-op
              }
            }
          });

      if (eventAction != null) {
        // Also notify SHIFTS feed of modified shift
        notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.SHIFTS, null, null, null, event.getShift().getId());
      }
    });
  }

  @EventListener(DriverLocationSetEvent.class)
  public void notifyDriverLocationSet(final DriverLocationSetEvent driverLocationSetEvent) {
    CompletableFuture.runAsync(() -> {
      notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.DRIVER_LOCATION, driverLocationSetEvent.getDriverId().toString(), null, null, driverLocationSetEvent.getLocation());
      notifyService.pushToSubscriptionAndSendNotification(null, false, true, NotifyService.Topic.DRIVER_LOCATIONS, null, null, null, driverLocationSetEvent);
    });
  }
}
