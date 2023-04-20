package com.rubyride.tripmanager.utility;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;

public final class ObjectUtils {
  private ObjectUtils() {
  }

  public static <T> T getOrDefault(@Nullable final T value, @NonNull final T defaultValue) {
    return Optional.ofNullable(value)
        .orElse(defaultValue);
  }

  public static Optional<Double> convert(final OptionalDouble optionalDouble) {
    return optionalDouble.isPresent() ?
        Optional.of(optionalDouble.getAsDouble()) :
        Optional.empty();
  }
}
