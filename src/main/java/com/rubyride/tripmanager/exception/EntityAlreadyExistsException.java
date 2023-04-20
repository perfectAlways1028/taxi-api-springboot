package com.rubyride.tripmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class EntityAlreadyExistsException extends RuntimeException {
  public EntityAlreadyExistsException(final String reason) {
    super(reason);
  }
}
