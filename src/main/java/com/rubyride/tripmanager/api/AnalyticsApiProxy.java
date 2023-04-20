package com.rubyride.tripmanager.api;

import com.rubyride.api.AnalyticsApi;
import com.rubyride.model.InlineResponse200;
import com.rubyride.model.TripRequestScheduleType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
public class AnalyticsApiProxy implements AnalyticsApi {
  private final AnalyticsApiImpl analyticsApiImpl;

  public AnalyticsApiProxy(final AnalyticsApiImpl analyticsApiImpl) {
    this.analyticsApiImpl = analyticsApiImpl;
  }

  @Override
  public ResponseEntity<InlineResponse200> getAggregatedTripCounts(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final UUID zoneId) {
    return analyticsApiImpl.getAggregatedTripCounts(from, to, zoneId);
  }

  @Override
  public ResponseEntity<Map<String, Long>> getTripCounts(@NotNull @Valid final LocalDate from, @NotNull @Valid final LocalDate to, @Valid final UUID zoneId, @Valid final TripRequestScheduleType scheduleType) {
    return analyticsApiImpl.getTripCounts(from, to, zoneId, scheduleType);
  }
}
