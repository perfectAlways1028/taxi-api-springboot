package com.rubyride.tripmanager.api;

import com.rubyride.api.ProcessingErrorsApi;
import com.rubyride.model.TripSchedulingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
public class ProcessingErrorsApiProxy implements ProcessingErrorsApi {
  private final ProcessingErrorsApiImpl processingErrorsApi;

  public ProcessingErrorsApiProxy(final ProcessingErrorsApiImpl processingErrorsApi) {
    this.processingErrorsApi = processingErrorsApi;
  }

  @Override
  public ResponseEntity<List<TripSchedulingException>> getTripProcessingErrors(final UUID zoneId, final OffsetDateTime startDate, final OffsetDateTime endDate) {
    return processingErrorsApi.getTripProcessingErrors(zoneId, startDate, endDate);
  }
}
