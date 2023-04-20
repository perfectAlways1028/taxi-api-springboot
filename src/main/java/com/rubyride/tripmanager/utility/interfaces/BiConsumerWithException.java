package com.rubyride.tripmanager.utility.interfaces;

@FunctionalInterface
public interface BiConsumerWithException<T, U, E extends Throwable> {
  void accept(T t, U u) throws E;
}