package com.rubyride.tripmanager.utility;

import com.rubyride.tripmanager.utility.interfaces.BiConsumerWithException;
import com.rubyride.tripmanager.utility.interfaces.BiFunctionWithException;
import com.rubyride.tripmanager.utility.interfaces.ConsumerWithException;
import com.rubyride.tripmanager.utility.interfaces.FunctionWithException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamUtils {
  private StreamUtils() {
  }

  public static <T> Stream<T> safeStream(final T[] array) {
    return Stream.ofNullable(array)
        .flatMap(Arrays::stream);
  }

  public static <T> Stream<T> safeStream(final Collection<T> collection) {
    return Stream.ofNullable(collection)
        .flatMap(Collection::stream);
  }

  public static <T> Stream<T> streamIterable(final Iterable<T> iterable) {
    return StreamSupport.stream(iterable.spliterator(), false);
  }

  public static <T> Stream<T> streamIterator(final Iterator<T> iterator) {
    final Iterable<T> iterable = () -> iterator;
    return streamIterable(iterable);
  }

  public static <T, R, E extends Exception> Function<T, R> uncheckFunction(final FunctionWithException<T, R, E> fe) {
    return arg -> {
      try {
        return fe.apply(arg);
      } catch (final Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T, U, R, E extends Exception> BiFunction<T, U, R> uncheckFunction(final BiFunctionWithException<T, U, R, E> bfe) {
    return (arg1, arg2) -> {
      try {
        return bfe.apply(arg1, arg2);
      } catch (final Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T, E extends Exception> Consumer<T> uncheckConsumer(final ConsumerWithException<T, E> ce) {
    return arg -> {
      try {
        ce.accept(arg);
      } catch (final Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <T, U, E extends Exception> BiConsumer<T, U> uncheckConsumer(final BiConsumerWithException<T, U, E> bce) {
    return (arg1, arg2) -> {
      try {
        bce.accept(arg1, arg2);
      } catch (final Throwable e) {
        throw new RuntimeException(e);
      }
    };
  }

  @SafeVarargs
  public static <T> Stream<T> merge(final Stream<T>... streams) {
    if (streams == null || streams.length == 0) {
      return Stream.empty();
    } else {
      final var streamIterator = Arrays.stream(streams).iterator();
      var output = streamIterator.next();

      while (streamIterator.hasNext()) {
        output = Stream.concat(output, streamIterator.next());
      }

      return output;
    }
  }

  public static <T> Predicate<T> not(final Predicate<T> predicate) {
    return predicate.negate();
  }
}
