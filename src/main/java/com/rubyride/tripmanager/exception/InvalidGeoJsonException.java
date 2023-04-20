package com.rubyride.tripmanager.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY)
public class InvalidGeoJsonException extends RuntimeException {
  public InvalidGeoJsonException(final JsonProcessingException exception) {
    super("Invalid GeoJSON", exception);
  }
}
