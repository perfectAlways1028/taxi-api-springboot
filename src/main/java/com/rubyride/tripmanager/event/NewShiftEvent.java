package com.rubyride.tripmanager.event;

import com.rubyride.model.Shift;

public class NewShiftEvent {
  private final Shift shift;

  public NewShiftEvent(final Shift shift) {
    this.shift = shift;
  }

  public Shift getShift() {
    return shift;
  }
}
