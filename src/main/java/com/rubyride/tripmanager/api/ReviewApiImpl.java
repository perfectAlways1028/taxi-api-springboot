package com.rubyride.tripmanager.api;

import com.rubyride.api.ReviewApi;
import com.rubyride.model.TripReview;
import com.rubyride.model.TripReviewWithCustomer;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.DriverReviewRepository;
import com.rubyride.tripmanager.repository.mongo.RiderReviewRepository;
import com.rubyride.tripmanager.repository.mongo.TripArchiveRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.utility.ObjectUtils;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
public class ReviewApiImpl implements ReviewApi {
  private static final Logger log = LogManager.getLogger(ReviewApiImpl.class);

  private final DriverReviewRepository driverReviewRepository;
  private final RiderReviewRepository riderReviewRepository;
  private final TripRepository tripRepository;
  private final TripArchiveRepository tripArchiveRepository;
  private final UserRepository userRepository;

  @Autowired
  public ReviewApiImpl(final DriverReviewRepository driverReviewRepository, final RiderReviewRepository riderReviewRepository, final TripRepository tripRepository, final TripArchiveRepository tripArchiveRepository, final UserRepository userRepository) {
    this.driverReviewRepository = driverReviewRepository;
    this.riderReviewRepository = riderReviewRepository;
    this.tripRepository = tripRepository;
    this.tripArchiveRepository = tripArchiveRepository;
    this.userRepository = userRepository;
  }

  private static void updateReview(final TripReview tripReview, final TripReview existingTripReview) {
    if (tripReview.getTripId() != null) {
      existingTripReview.setTripId(tripReview.getTripId());
    }

    if (tripReview.getDriverId() != null) {
      existingTripReview.setDriverId(tripReview.getDriverId());
    }

    if (tripReview.getRiderId() != null) {
      existingTripReview.setRiderId(tripReview.getRiderId());
    }

    if (tripReview.getRating() != null) {
      existingTripReview.setRating(tripReview.getRating());
    }

    if (tripReview.getProblem() != null) {
      existingTripReview.setProblem(tripReview.getProblem());
    }

    if (tripReview.getAdditionalInformation() != null) {
      existingTripReview.setAdditionalInformation(tripReview.getAdditionalInformation());
    }
  }

  @Override
  public ResponseEntity<Void> deleteDriverReview(final UUID reviewId) {
    driverReviewRepository.findById(reviewId)
        .ifPresent(tripReview -> driverReviewRepository.deleteById(reviewId));

    return ResponseEntity.noContent()
        .build();
  }

  @Override
  public ResponseEntity<Void> deleteRiderReview(final UUID reviewId) {
    riderReviewRepository.findById(reviewId)
        .ifPresent(tripReview -> riderReviewRepository.deleteById(reviewId));

    return ResponseEntity.noContent()
        .build();
  }

  @Override
  public ResponseEntity<List<TripReview>> getDriverReview(final List<UUID> reviewId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(driverReviewRepository.findAllById(reviewId))
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<TripReviewWithCustomer> getDriverReviewForTrip(final UUID tripId) {
    return ResponseEntity.of(getCustomerName(driverReviewRepository.findByTripId(tripId)));
  }

  @Override
  public ResponseEntity<List<TripReview>> getDriverReviews() {
    return ResponseEntity.ok(driverReviewRepository.findAll());
  }

  private Optional<TripReviewWithCustomer> getCustomerName(final Optional<TripReview> tripReviewMaybe) {
    return tripReviewMaybe
        .flatMap(tripReview -> userRepository.findById(tripReview.getRiderId())
            .map(rider -> new TripReviewWithCustomer()
                .id(tripReview.getId())
                .tripId(tripReview.getTripId())
                .driverId(tripReview.getDriverId())
                .riderId(tripReview.getRiderId())
                .rating(tripReview.getRating())
                .problem(tripReview.getProblem())
                .additionalInformation(tripReview.getAdditionalInformation())
                .customerFirstName(rider.getFirstName())
                .customerLastName(rider.getLastName())));
  }

  @Override
  public ResponseEntity<Double> getRatingForDriver(final UUID driverId) {
    return ResponseEntity.of(ObjectUtils.convert(
        StreamUtils.safeStream(driverReviewRepository.findByDriverId(driverId))
            .map(TripReview::getRating)
            .mapToDouble(Integer::doubleValue)
            .average()));
  }

  @Override
  public ResponseEntity<Double> getRatingForRider(final UUID riderId) {
    return ResponseEntity.of(ObjectUtils.convert(
        StreamUtils.safeStream(riderReviewRepository.findByRiderId(riderId))
            .map(TripReview::getRating)
            .mapToDouble(Integer::doubleValue)
            .average()));
  }

  @Override
  public ResponseEntity<List<TripReviewWithCustomer>> getReviewsForDriver(final UUID driverId) {
    return ResponseEntity.ok(driverReviewRepository.findByDriverId(driverId).stream()
        .map(Optional::of)
        .map(this::getCustomerName)
        .flatMap(Optional::stream)
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<List<TripReview>> getReviewsForRider(final UUID riderId) {
    return ResponseEntity.ok(riderReviewRepository.findByRiderId(riderId));
  }

  @Override
  public ResponseEntity<List<TripReview>> getRiderReview(final List<UUID> reviewId) {
    return ResponseEntity.ok(StreamUtils.streamIterable(riderReviewRepository.findAllById(reviewId))
        .collect(Collectors.toList()));
  }

  @Override
  public ResponseEntity<TripReview> getRiderReviewForTrip(final UUID tripId) {
    return ResponseEntity.of(riderReviewRepository.findByTripId(tripId));
  }

  @Override
  public ResponseEntity<List<TripReview>> getRiderReviews() {
    return ResponseEntity.ok(riderReviewRepository.findAll());
  }

  @Override
  public ResponseEntity<TripReview> submitDriverReview(@Valid final TripReview tripReview) {
    tripReview.setId(UUID.randomUUID());

    driverReviewRepository.insert(tripReview);

    return ResponseEntity.created(URI.create("/v1/reviews/driver/" + tripReview.getId().toString()))
        .body(tripReview);
  }

  @Override
  public ResponseEntity<TripReview> submitRiderReview(@Valid final TripReview tripReview) {
    tripReview.setId(UUID.randomUUID());

    riderReviewRepository.insert(tripReview);

    return ResponseEntity.created(URI.create("/v1/reviews/rider/" + tripReview.getId().toString()))
        .body(tripReview);
  }

  @Override
  public ResponseEntity<TripReview> updateDriverReview(@Valid final TripReview tripReview) {
    return driverReviewRepository.findById(tripReview.getId())
        .map(existingTripReview -> {
          updateReview(tripReview, existingTripReview);
          driverReviewRepository.save(existingTripReview);

          return ResponseEntity.ok()
              .location(URI.create("/v1/reviews/driver/" + existingTripReview.getId().toString()))
              .body(existingTripReview);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip review not found"));
  }

  @Override
  public ResponseEntity<TripReview> updateRiderReview(@Valid final TripReview tripReview) {
    return riderReviewRepository.findById(tripReview.getId())
        .map(existingTripReview -> {
          updateReview(tripReview, existingTripReview);
          riderReviewRepository.save(existingTripReview);

          return ResponseEntity.ok()
              .location(URI.create("/v1/reviews/driver/" + existingTripReview.getId().toString()))
              .body(existingTripReview);
        })
        .orElseThrow(() -> new EntityNotFoundException("Trip review not found"));
  }
}
