package com.rubyride.tripmanager.utility;

import com.rubyride.model.Event;
import com.rubyride.model.Shift;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AnalyticsUtils {
  public String getAnalytics(final List<Shift> shifts) {
    final List<AnalyticsRow> output = new ArrayList<>();

    shifts.forEach(shift -> {
      final var completeEventIterator = shift.getEvents().stream()
          .filter(event -> Boolean.TRUE.equals(event.getComplete()))
          .iterator();

      if (!completeEventIterator.hasNext()) {
        return;
      }

      var currentPassengers = 0;

      Event currentEvent = completeEventIterator.next(),
          nextEvent = completeEventIterator.next();

      while (currentPassengers == 0 && completeEventIterator.hasNext()) {
        currentEvent = nextEvent;
        nextEvent = completeEventIterator.next();

        currentPassengers += currentEvent.getPassengerDelta();
      }

      output.add(new AnalyticsRow(
          shift.getDriverId(),
          TimePeriodType.P1,
          currentEvent.getTime().toEpochSecond(),
          nextEvent.getTime().toEpochSecond()
      ));

      while (completeEventIterator.hasNext()) {
        currentEvent = nextEvent;
        nextEvent = completeEventIterator.next();

        currentPassengers += currentEvent.getPassengerDelta();

        output.add(new AnalyticsRow(
            shift.getDriverId(),
            currentPassengers > 0 ? TimePeriodType.P3 :
                TimePeriodType.P2,
            currentEvent.getTime().toEpochSecond(),
            nextEvent.getTime().toEpochSecond()
        ));
      }
    });

    return "driver_id,period,start_timestamp,end_timestamp" +
        output.stream()
            .map(AnalyticsRow::toCsv)
            .collect(Collectors.joining("\n"));
  }

  private enum TimePeriodType {
    P1,
    P2,
    P3,
    INVALID
//
//    public static TimePeriodType getForTransition(final EventAction currentAction, final EventAction nextAction) {
//      switch (currentAction) {
//        case SHIFT_START:
//          switch (nextAction) {
//            case SHIFT_END:
//              return P1;
//
//            case DRIVER_EN_ROUTE:
//            case PICKUP_ARRIVAL:
//            case PICKUP:
//              return P2;
//
//            case DROPOFF_ARRIVAL:
//            case DROPOFF:
//              return INVALID;
//          }
//
//        case DRIVER_EN_ROUTE: {
//          switch (nextAction) {
//            case SHIFT_START:
//            case SHIFT_END:
//            case DRIVER_EN_ROUTE:
//            case DROPOFF_ARRIVAL:
//            case DROPOFF:
//              return INVALID;
//
//            case PICKUP_ARRIVAL:
//            case PICKUP:
//              return P2;
//          }
//        }
//
//        case
//      }
//    }
  }

  private final class AnalyticsRow {
    private final UUID driverId;
    private final TimePeriodType timePeriod;
    private final long timePeriodStartTime;
    private final long timePeriodEndTime;

    private AnalyticsRow(final UUID driverId, final TimePeriodType timePeriod, final long timePeriodStartTime, final long timePeriodEndTime) {
      this.driverId = driverId;
      this.timePeriod = timePeriod;
      this.timePeriodStartTime = timePeriodStartTime;
      this.timePeriodEndTime = timePeriodEndTime;
    }

    private String toCsv() {
      return driverId.toString() + "," + timePeriod.name() + "," + timePeriodStartTime + "," + timePeriodEndTime;
    }
  }
}
