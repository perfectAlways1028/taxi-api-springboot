package com.rubyride.tripmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class UserIdMissingException extends RuntimeException {
  public UserIdMissingException(final String reason) {
    super(reason);
  }
}
