package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Driver;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DriverRepository extends MongoRepository<Driver, UUID> {
  Driver findByUserId(UUID userId);

  List<Driver> findByHomeZone(UUID zoneId);

  List<Driver> findByAssignedZone(UUID zoneId);
}
