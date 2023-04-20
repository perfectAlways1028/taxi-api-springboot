package com.rubyride.tripmanager.api;

import com.rubyride.model.Place;
import com.rubyride.model.Role;
import com.rubyride.model.User;
import com.rubyride.model.UserCredentials;
import com.rubyride.tripmanager.exception.EntityAlreadyExistsException;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.security.AccessControl;
import com.rubyride.tripmanager.security.TokenManager;
import com.rubyride.tripmanager.service.EmailService;
import com.rubyride.tripmanager.utility.ObjectUtils;
import com.rubyride.tripmanager.utility.SpringContext;
import com.rubyride.tripmanager.utility.StreamUtils;
import com.rubyride.tripmanager.utility.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UserApiImpl {
  private static final Logger log = LoggerFactory.getLogger(UserApiImpl.class);

  private final AccessControl accessControl;
  private final UserRepository userRepository;
  private final PlaceRepository placeRepository;
  private final TokenManager tokenManager;
  private final EmailService eMailService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final UserUtils userUtils;
  private final SpringContext springContext;

  public UserApiImpl(final AccessControl accessControl, final UserRepository userRepository, final PlaceRepository placeRepository, final TokenManager tokenManager, final EmailService eMailService, final BCryptPasswordEncoder bCryptPasswordEncoder, final SpringContext springContext, final UserUtils userUtils) {
    this.accessControl = accessControl;
    this.userRepository = userRepository;
    this.placeRepository = placeRepository;
    this.tokenManager = tokenManager;
    this.eMailService = eMailService;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    this.userUtils = userUtils;
    this.springContext = springContext;
  }

  private static User sanitizeOutput(final List<Role> roles, final User user, final boolean isSelf) {
    // always remove password from output
    user.password(null);

    // sanitize down minimal info if not self
    if (!isSelf) {
      if (!roles.contains(Role.ADMIN)) {
        user.created(null)
            .email(null)
            .places(null)
            .placeNicknames(null)
            .favoritePlaces(null)
            .userName(null);

        if (!CollectionUtils.containsAny(roles, List.of(Role.DISPATCHER, Role.DRIVER))) {
          user.active(null)
              .zones(null)
              .roles(null)
              .groups(null)
              .primaryPhone(null)
              .otherPhone(null);
        }
      }
    }

    return user;
  }

  private boolean isSelf(final User user) {
    return Optional.ofNullable(user.getUserName())
        .equals(springContext.getAuthenticatedUserName());
  }

  public ResponseEntity<User> addUser(@Valid final User user) {
    final var existingUser = userRepository.findByUserName(user.getUserName());
    if (existingUser != null) {
      throw new EntityAlreadyExistsException("User already exists");
    }

    if (ObjectUtils.getOrDefault(user.getRoles(), new ArrayList<>())
        .contains(Role.ADMIN)) {
      throw new AccessDeniedException("Cannot create an admin user");
    }

    user.setId(UUID.randomUUID());
    user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
    user.setCreated(OffsetDateTime.now());

    userRepository.insert(user);

    return ResponseEntity.created(URI.create("/v1/users/" + user.getId().toString()))
        .body(UserApiImpl.sanitizeOutput(Collections.emptyList(), user, true));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<Void> deleteUser(final UUID userId) {
    tokenManager.revokeTokens(userId);
    userRepository.deleteById(userId);

    return ResponseEntity.noContent()
        .build();
  }

  @PreAuthorize("@accessControl.canWriteUser(#user.id)")
  public ResponseEntity<User> updateUser(@Valid final User user) {
    return userRepository.findById(user.getId())
        .map(existingUser -> {
          if (user.getUserName() != null && !user.getUserName().equals(existingUser.getUserName())) {
            if (userRepository.findByUserName(user.getUserName()) != null) {
              throw new EntityAlreadyExistsException("User already exists");
            }

            existingUser.setUserName(user.getUserName());
            tokenManager.revokeTokens(user.getId());
          }

          if (user.getFirstName() != null) {
            existingUser.setFirstName(user.getFirstName());
          }

          if (user.getLastName() != null) {
            existingUser.setLastName(user.getLastName());
          }

          if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
          }

          if (user.getAddress() != null) {
            existingUser.setAddress(user.getAddress());
          }

          if (user.getPrimaryPhone() != null) {
            existingUser.setPrimaryPhone(user.getPrimaryPhone());
          }

          if (user.getOtherPhone() != null) {
            existingUser.setOtherPhone(user.getOtherPhone());
          }

          if (user.getNotificationType() != null) {
            existingUser.setNotificationType(user.getNotificationType());
          }

          if (user.getGroups() != null) {
            existingUser.setGroups(user.getGroups());
          }

          if (user.getRoles() != null) {
            final var distinctRoles = new HashSet<>(user.getRoles());
            final var existingDistinctRoles = ObjectUtils.getOrDefault(existingUser.getRoles(), Collections.<Role>emptySet());
            if (!distinctRoles.containsAll(existingDistinctRoles) ||
                !existingDistinctRoles.containsAll(distinctRoles)) {
              // Don't allow a user to set greater roles than currently authenticated user
              // ADMIN > DISPATCHER > PARTNER > DRIVER > RIDER
              final var maxAuthenticatedRole = Math.min(
                  springContext.getAuthenticatedUserRoles().stream()
                      .mapToInt(Role::ordinal)
                      .max()
                      .orElse(Role.DRIVER.ordinal()),
                  Role.DRIVER.ordinal());
              final var maxNewRole = user.getRoles().stream()
                  .mapToInt(Role::ordinal)
                  .max()
                  .orElse(Role.RIDER.ordinal());

              if (maxNewRole > maxAuthenticatedRole) {
                throw new AccessDeniedException("Cannot assign a role with greater access than currently authenticated user");
              }

              existingUser.setRoles(user.getRoles());
              tokenManager.revokeTokens(user.getId());
            }
          }

          if (user.getPartnerId() != null) {
            existingUser.setPartnerId(user.getPartnerId());
          }

          if (user.getActive() != null) {
            existingUser.setActive(user.getActive());
          }

          if (user.getZones() != null) {
            existingUser.setZones(user.getZones());
          }

          if (user.getPlaces() != null) {
            existingUser.setPlaces(user.getPlaces());
          }

          if (user.getPlaceNicknames() != null) {
            existingUser.setPlaceNicknames(user.getPlaceNicknames());
          }

          if (user.getResetPassword() != null) {
            existingUser.setResetPassword(user.getResetPassword());
          }

          userRepository.save(existingUser);

          return ResponseEntity.ok()
              .location(URI.create("/v1/users/" + existingUser.getId().toString()))
              .body(UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), user, isSelf(existingUser)));
        })
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  public ResponseEntity<List<User>> getUserById(final List<UUID> userId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(userRepository.findAllById(userId))
        .filter(user -> accessControl.canReadUser(user.getId()))
        .map(user -> UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), user, isSelf(user)))
        .collect(Collectors.toList()));
  }

  @PreAuthorize("hasAuthority('ADMIN')")
  public ResponseEntity<User> getUserByName(final String userName) {
    return Optional.ofNullable(userRepository.findByUserName(userName))
        .map(user -> ResponseEntity.of(
            Optional.of(UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), user, isSelf(user)))))
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  public ResponseEntity<Boolean> userAlreadyExists(final String userName) {
    return ResponseEntity.of(Optional.of(userUtils.userAlreadyExists(userName)));
  }

  private User getUser(final UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> addGroupToUser(final UUID userId, @NotNull @Valid final UUID groupId) {
    return updateUser(getUser(userId).addGroupsItem(groupId));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> addPlaceToUser(final UUID userId, @NotNull @Valid final UUID placeId) {
    return updateUser(getUser(userId).addPlacesItem(placeId));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> addZoneToUser(final UUID userId, @NotNull @Valid final UUID zoneId) {
    return updateUser(getUser(userId).addZonesItem(zoneId));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> deleteGroupFromUser(final UUID userId, final UUID groupId) {
    final var user = getUser(userId);

    final var groups = user.getGroups();
    groups.remove(groupId);

    return updateUser(user.groups(groups));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> deletePlaceFromUser(final UUID userId, final UUID placeId) {
    final var user = getUser(userId);

    final var places = user.getPlaces();
    places.remove(placeId);

    return updateUser(user.places(places));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> deleteZoneFromUser(final UUID userId, final UUID zoneId) {
    final var user = getUser(userId);

    final var zones = user.getZones();
    zones.remove(zoneId);

    return updateUser(user.zones(zones));
  }

  @PreAuthorize("@accessControl.canReadUser(#userId)")
  public ResponseEntity<List<Place>> getPlacesForUser(final UUID userId) {
    final var user = getUser(userId);
    final var placeNicknames = ObjectUtils.getOrDefault(user.getPlaceNicknames(), Collections.<String, String>emptyMap());

    return ResponseEntity.ok()
        .body(StreamUtils.safeStream(user.getPlaces())
            .map(placeRepository::findById)
            .flatMap(Optional::stream)
            .map(place -> place.name(placeNicknames.getOrDefault(
                place.getId().toString(),
                place.getName())))
            .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> setPassword(final UUID userId, @Valid final String password) {
    return userRepository.findById(userId)
        .map(existingUser -> {
          final var encryptedPassword = bCryptPasswordEncoder.encode(password);

          return setPassword(existingUser, encryptedPassword);
        })
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  @NotNull
  private ResponseEntity<User> setPassword(final User existingUser, final String encryptedPassword) {
    if (!encryptedPassword.equals(existingUser.getPassword())) {
      existingUser.setPassword(encryptedPassword);
      userRepository.save(existingUser);
      tokenManager.revokeTokens(existingUser.getUserName());
    }

    return ResponseEntity.ok()
        .location(URI.create("/v1/users/" + existingUser.getId().toString()))
        .body(UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), existingUser, isSelf(existingUser)));
  }

  @PreAuthorize("authentication.principal.equals(#userCredentials.getUserName()) or hasAuthority('ADMIN')")
  public ResponseEntity<User> setCredentials(final UserCredentials userCredentials) {
    return Optional.ofNullable(userRepository.findByUserName(userCredentials.getUserName()))
        .map(existingUser -> {
          final var encryptedPassword = bCryptPasswordEncoder.encode(userCredentials.getPassword());

          return setPassword(existingUser, encryptedPassword);
        })
        .orElseThrow(() -> new EntityNotFoundException("User not found"));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> setPlaceNicknameForUser(final UUID userId, final UUID placeId, @NotNull @Valid final String nickname) {
    final var user = getUser(userId);

    final var placeNicknames = ObjectUtils.getOrDefault(user.getPlaceNicknames(), new HashMap<String, String>());
    placeNicknames.put(placeId.toString(), nickname);

    return updateUser(user.placeNicknames(placeNicknames));
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> setFavoritePlaceForUser(final UUID userId, final UUID placeId, @NotNull @Valid final Boolean favorite) {
    final var user = getUser(userId);

    final var favoritePlaces = ObjectUtils.getOrDefault(user.getFavoritePlaces(), new ArrayList<>());

    if (favorite && !favoritePlaces.contains(placeId)) {
      return updateUser(user.addFavoritePlacesItem(placeId));
    } else if (!favorite && favoritePlaces.contains(placeId)) {
      favoritePlaces.remove(placeId);
      return updateUser(user);
    } else {
      // no-op
      return ResponseEntity.ok()
          .location(URI.create("/v1/users/" + user.getId().toString()))
          .body(UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), user, isSelf(user)));
    }
  }

  @PreAuthorize("@accessControl.canWriteUser(#userId)")
  public ResponseEntity<User> setUserActive(final UUID userId, final Boolean active) {
    return updateUser(getUser(userId).active(active));
  }

  @PreAuthorize("@accessControl.canAccessUsers()")
  public ResponseEntity<List<User>> getUsersForZone(final UUID zoneId, final List<Role> roles) {
    final var roleList = ObjectUtils.getOrDefault(roles, Collections.emptyList());

    final Stream<User> users;

    if (zoneId == null) {
      users = StreamUtils.streamIterable(userRepository.findAll())
          .filter(user -> ObjectUtils.getOrDefault(user.getZones(), Collections.emptySet()).isEmpty());
    } else {
      users = userRepository.findAllByZonesContaining(zoneId)
          .stream();
    }
    return ResponseEntity.ok(users
        .filter(user -> roleList.isEmpty() || roleList.stream()
            .anyMatch(ObjectUtils.getOrDefault(user.getRoles(), Collections.emptyList())::contains))
        .map(user -> UserApiImpl.sanitizeOutput(springContext.getAuthenticatedUserRoles(), user, isSelf(user)))
        .collect(Collectors.toList()));
  }
}
