package com.rubyride.tripmanager.utility.interfaces;

@FunctionalInterface
public interface BiFunctionWithException<T, U, R, E extends Throwable> {
  R apply(T t, U u) throws E;
}
