package com.rubyride.tripmanager.model;

import com.rubyride.model.TripRequestStatus;

import java.util.UUID;

public class TripRequestIdAndStatus {
  private final UUID id;
  private final TripRequestStatus status;

  public TripRequestIdAndStatus(final UUID id, final TripRequestStatus status) {
    this.id = id;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public TripRequestStatus getStatus() {
    return status;
  }
}
