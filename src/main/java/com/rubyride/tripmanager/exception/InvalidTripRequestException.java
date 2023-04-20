package com.rubyride.tripmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public final class InvalidTripRequestException extends RuntimeException {
  public InvalidTripRequestException(final String reason) {
    super(reason);
  }
}
