package com.rubyride.tripmanager.event;

import com.rubyride.model.EventAction;
import com.rubyride.model.TripRequest;

public class ModifyTripRequestEvent {
  private final TripRequest tripRequest;
  private final EventAction eventAction;

  public ModifyTripRequestEvent(final TripRequest tripRequest, final EventAction eventAction) {
    this.tripRequest = tripRequest;
    this.eventAction = eventAction;
  }

  public TripRequest getTripRequest() {
    return tripRequest;
  }

  public EventAction getEventAction() {
    return eventAction;
  }
}
