package com.rubyride.tripmanager.utility.interfaces;

@FunctionalInterface
public interface FunctionWithException<T, R, E extends Throwable> {
  R apply(T t) throws E;
}
