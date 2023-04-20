package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VehicleRepository extends MongoRepository<Vehicle, UUID> {
}
