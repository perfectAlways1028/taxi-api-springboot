package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.TripRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripArchiveRepository extends MongoRepository<TripRequest, UUID> {
  List<TripRequest> findByRiderId(UUID id);

  List<TripRequest> findByFromZoneId(UUID zoneId);

  List<TripRequest> findByToZoneId(UUID zoneId);
}
