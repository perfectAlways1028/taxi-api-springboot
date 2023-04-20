package com.rubyride.tripmanager.utility.interfaces;

@FunctionalInterface
public interface ConsumerWithException<T, E extends Throwable> {
  void accept(T t) throws E;
}