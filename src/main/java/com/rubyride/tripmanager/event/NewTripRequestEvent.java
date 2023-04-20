package com.rubyride.tripmanager.event;

import com.rubyride.model.TripRequest;

public class NewTripRequestEvent {
  private final TripRequest tripRequest;

  public NewTripRequestEvent(final TripRequest tripRequest) {
    this.tripRequest = tripRequest;
  }

  public TripRequest getTripRequest() {
    return tripRequest;
  }
}
