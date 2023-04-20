package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Shift;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftArchiveRepository extends MongoRepository<Shift, UUID> {
  List<Shift> findByDriverId(UUID driverId);
}
