package com.rubyride.tripmanager.event;

import com.rubyride.model.LatitudeLongitude;

import java.util.UUID;

public class DriverLocationSetEvent {
    private final UUID driverId;

    public UUID getDriverId() {
        return driverId;
    }

    public LatitudeLongitude getLocation() {
        return location;
    }

    private final LatitudeLongitude location;

    public DriverLocationSetEvent(final UUID driverId, final LatitudeLongitude location) {
        this.driverId = driverId;
        this.location = location;
    }
}
