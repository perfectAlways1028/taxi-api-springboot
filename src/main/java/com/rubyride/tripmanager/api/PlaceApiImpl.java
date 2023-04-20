package com.rubyride.tripmanager.api;

import com.rubyride.model.DataType;
import com.rubyride.model.Place;
import com.rubyride.model.Role;
import com.rubyride.model.User;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.DataBlobRepository;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.service.MapService;
import com.rubyride.tripmanager.utility.DataRepositoryUtils;
import com.rubyride.tripmanager.utility.ObjectUtils;
import com.rubyride.tripmanager.utility.SpringContext;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PlaceApiImpl {
  private final DataBlobRepository dataRepository;
  private final DataRepositoryUtils dataRepositoryUtils;
  private final MapService mapService;
  private final PlaceRepository placeRepository;
  private final UserRepository userRepository;
  private final SpringContext springContext;

  public PlaceApiImpl(final DataBlobRepository dataRepository, final DataRepositoryUtils dataRepositoryUtils, final MapService mapService, final PlaceRepository placeRepository, final UserRepository userRepository, final SpringContext springContext) {
    this.dataRepository = dataRepository;
    this.dataRepositoryUtils = dataRepositoryUtils;
    this.mapService = mapService;
    this.placeRepository = placeRepository;
    this.userRepository = userRepository;
    this.springContext = springContext;
  }

  public ResponseEntity<Place> addPlace(@Valid final Place place) {
    place.setId(UUID.randomUUID());

    final Optional<User> user;

    if (place.getUserId() == null) {
      user = springContext.getAuthenticatedUser();
    } else {
      user = userRepository.findById(place.getUserId());
    }

    return user
        .map(u -> {
          if (place.getLocation() == null ||
              place.getLocation().getLatitude() == null || place.getLocation().getLatitude() == 0D ||
          place.getLocation().getLongitude() == null || place.getLocation().getLongitude() == 0D) {
            mapService.geocode(place);
          }

          placeRepository.insert(place.userId(u.getId()));
          userRepository.save(u.addPlacesItem(place.getId()));

          return ResponseEntity.created(URI.create("/v1/places/" + place.getId().toString()))
              .body(place);
        })
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  public ResponseEntity<Void> deletePlace(final UUID placeId) {
    placeRepository.deleteById(placeId);

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<List<Place>> getAllPlacesForUser(final UUID userId) {
    return userRepository.findById(userId)
        .map(user -> {
          final var placeNicknames = ObjectUtils.getOrDefault(user.getPlaceNicknames(), Collections.<String, String>emptyMap());

          return ResponseEntity.ok(Stream.concat(
              StreamUtils.safeStream(user.getPlaces())
                  .map(placeRepository::findById)
                  .flatMap(Optional::stream),
              StreamUtils.safeStream(user.getZones())
                  .map(placeRepository::findByZoneId)
                  .flatMap(Collection::stream))
              .filter(place -> place.getUserId().equals(user.getId()) ||
                  CollectionUtils.containsAny(
                      ObjectUtils.getOrDefault(user.getRoles(), Collections.emptyList()),
                      List.of(Role.ADMIN, Role.DISPATCHER, Role.DRIVER)) ||
                  !place.getIsPrivate())
              .distinct()
              .map(place -> place.name(placeNicknames.getOrDefault(
                  place.getId().toString(),
                  place.getName())))
              .collect(Collectors.toList()));
        })
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  public ResponseEntity<List<Place>> getPlaceById(final List<UUID> placeId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(placeRepository.findAllById(placeId))
        .collect(Collectors.toList()));
  }

  public ResponseEntity<List<Place>> getPlaces(@Valid final Boolean selfOnly) {
    return springContext.getAuthenticatedUser()
        .map(user -> {
          final var placeNicknames = ObjectUtils.getOrDefault(user.getPlaceNicknames(), Collections.<String, String>emptyMap());

          return ResponseEntity.ok(StreamUtils.merge(
              StreamUtils.safeStream(user.getPlaces())
                  .map(placeRepository::findById)
                  .flatMap(Optional::stream),
              StreamUtils.safeStream(user.getZones())
                  .map(placeRepository::findByZoneId)
                  .flatMap(Collection::stream))
              .filter(place -> place.getUserId().equals(user.getId()) ||
                  ((user.getRoles().contains(Role.ADMIN) || !place.getIsPrivate())
                      && !ObjectUtils.getOrDefault(selfOnly, Boolean.FALSE)))
              .distinct()
              .map(place -> place.name(placeNicknames.getOrDefault(
                  place.getId().toString(),
                  place.getName())))
              .collect(Collectors.toList()));
        })
        .orElseThrow(() -> new EntityNotFoundException("User not authenticated"));
  }

  public ResponseEntity<List<Place>> getPlacesForGroup(final UUID groupId) {
    return ResponseEntity.ok(placeRepository.findAllByGroupsContaining(groupId));
  }

  public ResponseEntity<List<Place>> getPlacesForZone(final UUID zoneId) {
    return ResponseEntity.ok(placeRepository.findByZoneId(zoneId));
  }

  public ResponseEntity<Place> setPlaceIcon(final UUID placeId, final UUID iconId) {
    return placeRepository.findById(placeId)
        .map(place -> dataRepository.findById(iconId)
            .filter(data -> data.getType() == DataType.PLACE_ICON)
            .map(icon -> {
              dataRepositoryUtils.removeReference(place.getIconId(), placeId);
              dataRepositoryUtils.addReference(iconId, placeId);

              return updatePlace(place.iconId(iconId));
            })
            .orElseThrow(() -> new EntityNotFoundException("Icon not found")))
        .orElseThrow(() -> new EntityNotFoundException("Place not found"));
  }

  public ResponseEntity<Place> setPlaceImage(final UUID placeId, @Valid final byte[] image) {
    return placeRepository.findById(placeId)
        .map(place -> {
          dataRepositoryUtils.removeReference(place.getImageId(), place.getId());

          final var imageId = image.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.PLACE_IMAGE, image, place.getId()) :
              null;

          return updatePlace(place.imageId(imageId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Place not found"));
  }

  public ResponseEntity<Place> updatePlace(@Valid final Place place) {
    return placeRepository.findById(place.getId())
        .map(existingPlace -> {
          if (place.getName() != null) {
            existingPlace.setName(place.getName());
          }

          if (place.getDescription() != null) {
            existingPlace.setDescription(place.getDescription());
          }

          if (place.getType() != null) {
            existingPlace.setType(place.getType());
          }

          if (place.getIsPrivate() != null) {
            existingPlace.setIsPrivate((place.getIsPrivate()));
          }

          if (place.getLocation() != null) {
            existingPlace.setLocation(place.getLocation());
          }

          if (place.getAddress() != null && !Objects.equals(place.getAddress(), existingPlace.getAddress())) {
            existingPlace.setAddress(place.getAddress());
            mapService.geocode(existingPlace);
          }

          if (place.getHoursOfOperation() != null) {
            existingPlace.setHoursOfOperation(place.getHoursOfOperation());
          }

          if (place.getPhoneNumber() != null) {
            existingPlace.setPhoneNumber(place.getPhoneNumber());
          }

          if (place.getIconId() != null) {
            existingPlace.setIconId(place.getIconId());
          }

          if (place.getImageId() != null) {
            existingPlace.setImageId(place.getImageId());
          }

          if (place.getZoneId() != null) {
            existingPlace.setZoneId(place.getZoneId());
          }

          if (place.getGroups() != null) {
            existingPlace.setGroups(place.getGroups());
          }

          placeRepository.save(existingPlace);

          return ResponseEntity.ok()
              .location(URI.create("/v1/places/" + existingPlace.getId().toString()))
              .body(existingPlace);
        })
        .orElseThrow(() -> new EntityNotFoundException("Place not found"));
  }
}
