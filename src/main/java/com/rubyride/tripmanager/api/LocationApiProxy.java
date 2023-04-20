package com.rubyride.tripmanager.api;

import com.rubyride.api.LocationApi;
import com.rubyride.model.LatitudeLongitude;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class LocationApiProxy implements LocationApi {
  private final LocationApiImpl locationApi;

  public LocationApiProxy(final LocationApiImpl locationApi) {
    this.locationApi = locationApi;
  }

  @Override
  public ResponseEntity<LatitudeLongitude> getLocation(final UUID driverId) {
    return locationApi.getLocation(driverId);
  }
}
