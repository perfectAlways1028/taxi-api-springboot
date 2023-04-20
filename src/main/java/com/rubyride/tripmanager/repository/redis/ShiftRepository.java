package com.rubyride.tripmanager.repository.redis;

import com.rubyride.model.Shift;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends KeyValueRepository<Shift, UUID> {
  List<Shift> findByDriverId(UUID driverId);

  List<Shift> findByActive(boolean active);

  List<Shift> findByZoneId(UUID zoneId);
}
