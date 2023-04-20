package com.rubyride.tripmanager.api;

import com.rubyride.api.PlaceApi;
import com.rubyride.model.Place;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
public class PlaceApiProxy implements PlaceApi {
  private final PlaceApiImpl placeApi;

  @Autowired
  public PlaceApiProxy(final PlaceApiImpl placeApi) {
    this.placeApi = placeApi;
  }

  @Override
  public ResponseEntity<Place> addPlace(final Place place) {
    return placeApi.addPlace(place);
  }

  @Override
  public ResponseEntity<Void> deletePlace(final UUID placeId) {
    return placeApi.deletePlace(placeId);
  }

  @Override
  public ResponseEntity<List<Place>> getAllPlacesForUser(final UUID userId) {
    return placeApi.getAllPlacesForUser(userId);
  }

  @Override
  public ResponseEntity<List<Place>> getPlaceById(final List<UUID> placeId) {
    return placeApi.getPlaceById(placeId);
  }

  @Override
  public ResponseEntity<List<Place>> getPlaces(final Boolean selfOnly) {
    return placeApi.getPlaces(selfOnly);
  }

  @Override
  public ResponseEntity<List<Place>> getPlacesForGroup(final UUID groupId) {
    return placeApi.getPlacesForGroup(groupId);
  }

  @Override
  public ResponseEntity<List<Place>> getPlacesForZone(final UUID zoneId) {
    return placeApi.getPlacesForZone(zoneId);
  }

  @Override
  public ResponseEntity<Place> setPlaceIcon(final UUID placeId, final UUID iconId) {
    return placeApi.setPlaceIcon(placeId, iconId);
  }

  @Override
  public ResponseEntity<Place> setPlaceImage(final UUID placeId, @Valid final byte[] image) {
    return placeApi.setPlaceImage(placeId, image);
  }

  @Override
  public ResponseEntity<Place> updatePlace(final Place place) {
    return placeApi.updatePlace(place);
  }
}
