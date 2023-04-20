package com.rubyride.tripmanager.api;

import com.rubyride.api.VehicleApi;
import com.rubyride.model.Vehicle;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.VehicleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.UUID;

@RestController
public class VehicleApiImpl implements VehicleApi {
  private final VehicleRepository vehicleRepository;

  public VehicleApiImpl(final VehicleRepository vehicleRepository) {
    this.vehicleRepository = vehicleRepository;
  }

  @Override
  public ResponseEntity<Vehicle> addVehicle(@Valid final Vehicle vehicle) {
    vehicle.setId(UUID.randomUUID());

    vehicleRepository.insert(vehicle);

    return ResponseEntity.created(URI.create("/v1/vehicles/" + vehicle.getId().toString()))
        .body(vehicle);
  }

  @Override
  public ResponseEntity<Void> deleteVehicle(final UUID vehicleId) {
    vehicleRepository.deleteById(vehicleId);

    return ResponseEntity.noContent()
        .build();
  }

  @Override
  public ResponseEntity<Vehicle> getVehicleById(final UUID vehicleId) {
    return ResponseEntity.of(vehicleRepository.findById(vehicleId));
  }

  @Override
  public ResponseEntity<Vehicle> setVehicleImage(final UUID vehicleId, @Valid final byte[] image) {
    return vehicleRepository.findById(vehicleId)
        .map(vehicle -> updateVehicle(vehicle.image(image)))
        .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
  }

  @Override
  public ResponseEntity<Vehicle> updateVehicle(@Valid final Vehicle vehicle) {
    return vehicleRepository.findById(vehicle.getId())
        .map(existingVehicle -> {
          if (vehicle.getMake() != null) {
            existingVehicle.setMake(vehicle.getMake());
          }

          if (vehicle.getModel() != null) {
            existingVehicle.setModel(vehicle.getModel());
          }

          if (vehicle.getTrim() != null) {
            existingVehicle.setTrim(vehicle.getTrim());
          }

          if (vehicle.getYear() != null) {
            existingVehicle.setYear(vehicle.getYear());
          }

          if (vehicle.getPlate() != null) {
            existingVehicle.setPlate(vehicle.getPlate());
          }

          if (vehicle.getPlateState() != null) {
            existingVehicle.setPlateState(vehicle.getPlateState());
          }

          if (vehicle.getVin() != null) {
            existingVehicle.setVin(vehicle.getVin());
          }

          if (vehicle.getColor() != null) {
            existingVehicle.setColor(vehicle.getColor());
          }

          if (vehicle.getCapacity() != null) {
            existingVehicle.setCapacity(vehicle.getCapacity());
          }

          if (vehicle.getRegistrationType() != null) {
            existingVehicle.setRegistrationType(vehicle.getRegistrationType());
          }

          if (vehicle.getRegistrationExpiration() != null) {
            existingVehicle.setRegistrationExpiration(vehicle.getRegistrationExpiration());
          }

          if (vehicle.getInsuranceType() != null) {
            existingVehicle.setInsuranceType(vehicle.getInsuranceType());
          }

          if (vehicle.getInsuranceNamed() != null) {
            existingVehicle.setInsuranceNamed(vehicle.getInsuranceNamed());
          }

          if (vehicle.getImage() != null) {
            existingVehicle.setImage(vehicle.getImage());
          }

          vehicleRepository.save(existingVehicle);

          return ResponseEntity.ok()
              .location(URI.create("/v1/vehicles/" + existingVehicle.getId().toString()))
              .body(existingVehicle);
        })
        .orElseThrow(() -> new EntityNotFoundException("Vehicle not found"));
  }
}
