package com.rubyride.tripmanager.repository.redis;

import com.rubyride.model.TripRequest;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripRepository extends KeyValueRepository<TripRequest, UUID> {
  List<TripRequest> findByRiderId(UUID id);

  List<TripRequest> findByFromZoneId(UUID zoneId);

  List<TripRequest> findByToZoneId(UUID zoneId);
}
