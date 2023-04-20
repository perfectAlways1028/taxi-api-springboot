package com.rubyride.tripmanager.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class CyclicGroupsException extends RuntimeException {
  public CyclicGroupsException(final String reason) {
    super(reason);
  }
}
