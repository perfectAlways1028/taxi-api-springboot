package com.rubyride.tripmanager.api;

import com.rubyride.api.UserApi;
import com.rubyride.model.Place;
import com.rubyride.model.Role;
import com.rubyride.model.User;
import com.rubyride.model.UserCredentials;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@RestController
public class UserApiProxy implements UserApi {
  private final UserApiImpl userApi;

  public UserApiProxy(final UserApiImpl userApi) {
    this.userApi = userApi;
  }

  @Override
  public ResponseEntity<User> addGroupToUser(final UUID userId, @NotNull @Valid final UUID groupId) {
    return userApi.addGroupToUser(userId, groupId);
  }

  @Override
  public ResponseEntity<User> addPlaceToUser(final UUID userId, @NotNull @Valid final UUID placeId) {
    return userApi.addPlaceToUser(userId, placeId);
  }

  @Override
  public ResponseEntity<User> addUser(@Valid final User user) {
    return userApi.addUser(user);
  }

  @Override
  public ResponseEntity<User> addZoneToUser(final UUID userId, @NotNull @Valid final UUID zoneId) {
    return userApi.addZoneToUser(userId, zoneId);
  }

  @Override
  public ResponseEntity<User> deleteGroupFromUser(final UUID userId, final UUID groupId) {
    return userApi.deleteGroupFromUser(userId, groupId);
  }

  @Override
  public ResponseEntity<User> deletePlaceFromUser(final UUID userId, final UUID placeId) {
    return userApi.deletePlaceFromUser(userId, placeId);
  }

  @Override
  public ResponseEntity<Void> deleteUser(final UUID userId) {
    return userApi.deleteUser(userId);
  }

  @Override
  public ResponseEntity<User> deleteZoneFromUser(final UUID userId, final UUID zoneId) {
    return userApi.deleteZoneFromUser(userId, zoneId);
  }

  @Override
  public ResponseEntity<List<Place>> getPlacesForUser(final UUID userId) {
    return userApi.getPlacesForUser(userId);
  }

  @Override
  public ResponseEntity<List<User>> getUserById(final List<UUID> userId) {
    return userApi.getUserById(userId);
  }

  @Override
  public ResponseEntity<User> getUserByName(final String userName) {
    return userApi.getUserByName(userName);
  }

  @Override
  public ResponseEntity<List<User>> getUsersForZone(@Valid final UUID zoneId, @Valid final List<Role> roles) {
    return userApi.getUsersForZone(zoneId, roles);
  }

  @Override
  public ResponseEntity<User> setCredentials(@Valid final UserCredentials userCredentials) {
    return userApi.setCredentials(userCredentials);
  }

  @Override
  public ResponseEntity<User> setFavoritePlaceForUser(final UUID userId, final UUID placeId, @NotNull @Valid final Boolean favorite) {
    return userApi.setFavoritePlaceForUser(userId, placeId, favorite);
  }

  @Override
  public ResponseEntity<User> setPassword(final UUID userId, @Valid final String password) {
    return userApi.setPassword(userId, password);
  }

  @Override
  public ResponseEntity<User> setPlaceNicknameForUser(final UUID userId, final UUID placeId, @NotNull @Valid final String nickname) {
    return userApi.setPlaceNicknameForUser(userId, placeId, nickname);
  }

  @Override
  public ResponseEntity<User> setUserActive(final UUID userId, final Boolean active) {
    return userApi.setUserActive(userId, active);
  }

  @Override
  public ResponseEntity<User> updateUser(@Valid final User user) {
    return userApi.updateUser(user);
  }

  @Override
  public ResponseEntity<Boolean> userAlreadyExists(final String userName) {
    return userApi.userAlreadyExists(userName);
  }
}
