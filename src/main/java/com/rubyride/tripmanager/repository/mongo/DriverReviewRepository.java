package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.TripReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverReviewRepository extends MongoRepository<TripReview, UUID> {
  List<TripReview> findByDriverId(UUID driverId);

  Optional<TripReview> findByTripId(UUID tripId);
}
