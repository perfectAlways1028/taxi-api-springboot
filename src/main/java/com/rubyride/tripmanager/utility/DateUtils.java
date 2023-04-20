package com.rubyride.tripmanager.utility;

import org.springframework.data.util.Pair;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class DateUtils {
  private DateUtils() {
  }

  public static Pair<LocalDate, LocalDate> getMinMaxDates(final LocalDate from, final LocalDate to) {
    final var date1 = ObjectUtils.getOrDefault(from, LocalDate.MIN);
    final var date2 = ObjectUtils.getOrDefault(to, LocalDate.MAX);

    final var minDate = date1.compareTo(date2) < 0 ?
        date1 :
        date2;

    final var maxDate = date1.compareTo(date2) > 0 ?
        date1 :
        date2;

    return Pair.of(minDate, maxDate);
  }

  public static Pair<OffsetDateTime, OffsetDateTime> getMinMaxDates(final OffsetDateTime from, final OffsetDateTime to) {
    final var date1 = ObjectUtils.getOrDefault(from, OffsetDateTime.MIN);
    final var date2 = ObjectUtils.getOrDefault(to, OffsetDateTime.MAX);

    final var minDate = date1.compareTo(date2) < 0 ?
        date1 :
        date2;

    final var maxDate = date1.compareTo(date2) > 0 ?
        date1 :
        date2;

    return Pair.of(minDate, maxDate);
  }
}
