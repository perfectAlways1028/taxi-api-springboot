package com.rubyride.tripmanager.utility;

import com.rubyride.model.Event;
import com.rubyride.model.EventAction;
import com.rubyride.model.Shift;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.IntStream;

public final class SchedulingUtils {
  /**
   * Sort events so those with a set time are first, then the shift end event is last in the list;
   * call everything else equal so sort order is stable when new events are added or moved around in list
   */
  private static final Comparator<Event> eventComparator = (a, b) -> {
    if (a == b) {
      return 0;
    }
    if (a == null) {
      return 1;
    }
    if (b == null) {
      return -1;
    }

    // want SHIFT_START first and SHIFT_END last
    if (EventAction.SHIFT_START.equals(a.getAction())) {
      return -1;
    } else if (EventAction.SHIFT_START.equals(b.getAction())) {
      return 1;
    }

    if (EventAction.SHIFT_END.equals(a.getAction())) {
      return 1;
    } else if (EventAction.SHIFT_END.equals(b.getAction())) {
      return -1;
    }

    // sort by time ascending
    final var aTime = ObjectUtils.getOrDefault(a.getTime(), OffsetDateTime.MAX);
    final var bTime = ObjectUtils.getOrDefault(b.getTime(), OffsetDateTime.MAX);

    final var timeResult = aTime.compareTo(bTime);

    if (timeResult != 0) {
      return timeResult;
    }

    // then by complete results first
    final var aComplete = ObjectUtils.getOrDefault(a.getComplete(), Boolean.FALSE);
    final var bComplete = ObjectUtils.getOrDefault(b.getComplete(), Boolean.FALSE);

    return bComplete.compareTo(aComplete);
  };

  private SchedulingUtils() {
  }

  public static Shift addOrUpdateEvent(final Shift shift, final Event event, final UUID anchorEventId) {
    final var events = ObjectUtils.getOrDefault(shift.getEvents(), new ArrayList<Event>());

    if (event.getId() == null) {
      event.setId(UUID.randomUUID());
    }

    if (anchorEventId != null) {
      final var anchorEventPosition = IntStream.range(0, events.size())
          .filter(i -> org.springframework.util.ObjectUtils.nullSafeEquals(events.get(i).getId(), anchorEventId))
          .findFirst()
          .orElse(-1);

      if (anchorEventPosition != -1) {
        final var anchorEvent = events.get(anchorEventPosition);

        // If moving this event to after an anchor, set its time to *just* after it (if it's set) so ordering later on
        // will honor this
        if (anchorEvent.getTime() != null) {
          event.setTime(anchorEvent.getTime().plusNanos(1_000L));
        }

        // Remove event if it already exists and (re-)add at new position
        events.stream()
            .filter(e -> org.springframework.util.ObjectUtils.nullSafeEquals(event.getTripRequestId(), e.getTripRequestId()))
            .filter(e -> org.springframework.util.ObjectUtils.nullSafeEquals(event.getAction(), (e.getAction())))
            .findFirst()
            .ifPresent(events::remove);

        events.add(anchorEventPosition + 1, event);
      }
    }

    // now actually update event (definitely already exists at this point)
    events.stream()
        .filter(e -> org.springframework.util.ObjectUtils.nullSafeEquals(event.getTripRequestId(), e.getTripRequestId()))
        .filter(e -> org.springframework.util.ObjectUtils.nullSafeEquals(event.getAction(), (e.getAction())))
        .findFirst()
        .ifPresentOrElse(
            existingEvent -> updateEvent(existingEvent, event),
            () -> events.add(event));

    return sortEvents(shift.events(events));
  }

  public static Shift sortEvents(final Shift shift) {
    if (shift.getEvents() != null) {
      shift.getEvents().sort(eventComparator);
    }

    return shift;
  }

  private static Event updateEvent(final Event existingEvent, final Event event) {
    if (event.getTime() != null) {
      existingEvent.setTime(event.getTime());
    }

    if (event.getAction() != null) {
      existingEvent.setAction(event.getAction());
    }

    if (event.getRiderId() != null) {
      existingEvent.setRiderId(event.getRiderId());
    }

    if (event.getTripRequestId() != null) {
      existingEvent.setTripRequestId(event.getTripRequestId());
    }

    if (event.getPlaceId() != null) {
      existingEvent.setPlaceId(event.getPlaceId());
    }

    if (event.getLeftFloat() != null) {
      existingEvent.setLeftFloat(event.getLeftFloat());
    }

    if (event.getRightFloat() != null) {
      existingEvent.setRightFloat(event.getRightFloat());
    }

    if (event.getPassengerDelta() != null) {
      existingEvent.setPassengerDelta(event.getPassengerDelta());
    }

    if (event.getComplete() != null) {
      existingEvent.setComplete(event.getComplete());
    }

    return existingEvent;
  }
}
