package com.rubyride.tripmanager.api;

import com.rubyride.api.DriverApi;
import com.rubyride.model.Driver;
import com.rubyride.model.LatitudeLongitude;
import com.rubyride.model.Role;
import com.rubyride.tripmanager.exception.EntityAlreadyExistsException;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.DriverRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.service.LocationService;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class DriverApiImpl implements DriverApi {
  private final LocationService locationService;
  private final DriverRepository driverRepository;
  private final UserRepository userRepository;

  public DriverApiImpl(final LocationService locationService, final DriverRepository driverRepository, final UserRepository userRepository) {
    this.locationService = locationService;
    this.driverRepository = driverRepository;
    this.userRepository = userRepository;
  }

  @Override
  public ResponseEntity<Driver> addDriver(@Valid final Driver driver) {
    userRepository.findById(driver.getUserId())
        .ifPresentOrElse(user ->
                Optional.ofNullable(driverRepository.findByUserId(driver.getUserId()))
                    .ifPresentOrElse(existingDriver -> {
                          throw new EntityAlreadyExistsException("Driver already exists");
                        },
                        () -> {
                          driver.setId(UUID.randomUUID());
                          driverRepository.insert(driver);

                          if (StreamUtils.safeStream(user.getRoles())
                              .noneMatch(Role.DRIVER::equals)) {
                            userRepository.save(user.addRolesItem(Role.DRIVER));
                          }
                        }),
            () -> {
              throw new EntityNotFoundException("User not found");
            });

    return ResponseEntity.created(URI.create("/v1/drivers/" + driver.getId().toString()))
        .body(driver);
  }

  @Override
  public ResponseEntity<Void> deleteDriver(final UUID driverId) {
    driverRepository.findById(driverId)
        .ifPresent(driver -> {
          driverRepository.deleteById(driver.getId());

          userRepository.findById(driver.getUserId())
              .ifPresent(user -> userRepository.save(user
                  .roles(StreamUtils.safeStream(user.getRoles())
                      .filter(role -> !Role.DRIVER.equals(role))
                      .collect(Collectors.toList()))));
        });

    return ResponseEntity.noContent()
        .build();
  }

  @Override
  public ResponseEntity<Driver> getDriverById(final UUID driverId) {
    return ResponseEntity.of(driverRepository.findById(driverId));
  }

  @Override
  public ResponseEntity<List<Driver>> getAllDrivers() {
    return ResponseEntity.ok(StreamUtils.streamIterable(driverRepository.findAll())
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<Driver> getDriverByUserId(final UUID userId) {
    return ResponseEntity.of(
        Optional.ofNullable(driverRepository.findByUserId(userId)));
  }

  @Override
  public ResponseEntity<List<Driver>> getDriversForZone(final UUID zoneId) {
    return ResponseEntity.ok(Stream.of(
        userRepository.findAllByZonesContainingAndRolesContaining(zoneId, Role.DRIVER).stream()
            .map(user -> driverRepository.findByUserId(user.getId()))
            .filter(Objects::nonNull),
        driverRepository.findByHomeZone(zoneId).stream(),
        driverRepository.findByAssignedZone(zoneId).stream())
        .flatMap(Function.identity())
        .distinct()
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<Driver> setDriverOnDuty(final UUID driverId, final Boolean onDuty, final LatitudeLongitude location) {
    return driverRepository.findById(driverId)
        .map(StreamUtils.uncheckFunction(driver -> {
          if (Boolean.FALSE.equals(onDuty)) {
            locationService.flushLocation(driverId);
          } else {
            locationService.setLocation(driverId, location);
          }

          return updateDriver(driver.onDuty(onDuty));
        }))
        .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
  }

  @Override
  public ResponseEntity<Driver> setDriverImage(final UUID driverId, final byte[] image) {
    return driverRepository.findById(driverId)
        .map(driver -> updateDriver(driver.image(image)))
        .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
  }

  @Override
  public ResponseEntity<Driver> updateDriver(@Valid final Driver driver) {
    return driverRepository.findById(driver.getId())
        .map(existingDriver -> {
          if (driver.getAssignedZone() != null) {
            existingDriver.setAssignedZone(driver.getAssignedZone());
          }

          if (driver.getDriversLicense() != null) {
            existingDriver.setDriversLicense(driver.getDriversLicense());
          }

          if (driver.getDriversLicenseState() != null) {
            existingDriver.setDriversLicenseState(driver.getDriversLicenseState());
          }

          if (driver.getOnDuty() != null) {
            existingDriver.setOnDuty(driver.getOnDuty());
          }

          if (driver.getHireDate() != null) {
            existingDriver.setHireDate(driver.getHireDate());
          }

          if (driver.getHiredBy() != null) {
            existingDriver.setHiredBy(driver.getHiredBy());
          }

          if (driver.getHomeZone() != null) {
            existingDriver.setHomeZone(driver.getHomeZone());
          }

          if (driver.getVehicle() != null) {
            existingDriver.setVehicle(driver.getVehicle());
          }

          if (driver.getVaccinationStatus() != null) {
            existingDriver.setVaccinationStatus(driver.getVaccinationStatus());
          }

          driverRepository.save(existingDriver);

          return ResponseEntity.ok()
              .location(URI.create("/v1/drivers/" + existingDriver.getId().toString()))
              .body(existingDriver);
        })
        .orElseThrow(() -> new EntityNotFoundException("Driver not found"));
  }
}
