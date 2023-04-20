package com.rubyride.tripmanager.event;

import com.rubyride.model.EventAction;
import com.rubyride.model.Shift;
import com.rubyride.model.TripRequest;

public class ModifyShiftEvent {
  private final Shift shift;
  private final TripRequest tripRequest;
  private final EventAction eventAction;
  private final boolean dataOnly;

  public ModifyShiftEvent(final Shift shift, final TripRequest tripRequest, final EventAction eventAction, final boolean dataOnly) {
    this.shift = shift;
    this.tripRequest = tripRequest;
    this.eventAction = eventAction;
    this.dataOnly = dataOnly;
  }

  public Shift getShift() {
    return shift;
  }

  public TripRequest getTripRequest() {
    return tripRequest;
  }

  public EventAction getEventAction() {
    return eventAction;
  }

  public boolean isDataOnly() {
    return dataOnly;
  }
}
