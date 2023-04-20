package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.TripSchedulingException;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TripSchedulingExceptionRepository extends MongoRepository<TripSchedulingException, UUID> {
  List<TripSchedulingException> findByTimeBetween(OffsetDateTime from, OffsetDateTime to);
}
