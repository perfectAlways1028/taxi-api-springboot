package com.rubyride.tripmanager.utility;

import com.rubyride.tripmanager.repository.mongo.*;
import com.rubyride.tripmanager.repository.redis.ShiftRepository;
import com.rubyride.tripmanager.repository.redis.TripRepository;
import com.rubyride.tripmanager.repository.redis.UserTokensRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Function;

@Component
public class AdminUtils {
  private static final Logger log = LoggerFactory.getLogger(AdminUtils.class);

  private final ShiftRepository shiftRepository;
  private final TripRepository tripRepository;
  private final UserTokensRepository tokensRepository;

  private final DriverRepository driverRepository;
  private final DriverReviewRepository driverReviewRepository;
  private final GroupRepository groupRepository;
  private final PlaceRepository placeRepository;
  private final RiderReviewRepository riderReviewRepository;
  private final ShiftArchiveRepository shiftArchiveRepository;
  private final TripArchiveRepository tripArchiveRepository;
  private final UserRepository userRepository;
  private final VehicleRepository vehicleRepository;
  private final ZoneRepository zoneRepository;

  private final TripUtils tripUtils;

  public AdminUtils(final ShiftRepository shiftRepository, final TripRepository tripRepository, final UserTokensRepository tokensRepository, final DriverRepository driverRepository, final DriverReviewRepository driverReviewRepository, final GroupRepository groupRepository, final PlaceRepository placeRepository, final RiderReviewRepository riderReviewRepository, final ShiftArchiveRepository shiftArchiveRepository, final TripArchiveRepository tripArchiveRepository, final UserRepository userRepository, final VehicleRepository vehicleRepository, final ZoneRepository zoneRepository, final TripUtils tripUtils) {
    this.shiftRepository = shiftRepository;
    this.tripRepository = tripRepository;
    this.tokensRepository = tokensRepository;
    this.driverRepository = driverRepository;
    this.driverReviewRepository = driverReviewRepository;
    this.groupRepository = groupRepository;
    this.placeRepository = placeRepository;
    this.riderReviewRepository = riderReviewRepository;
    this.shiftArchiveRepository = shiftArchiveRepository;
    this.tripArchiveRepository = tripArchiveRepository;
    this.userRepository = userRepository;
    this.vehicleRepository = vehicleRepository;
    this.zoneRepository = zoneRepository;
    this.tripUtils = tripUtils;
  }

  private static <T> void reindexRepository(final CrudRepository<T, UUID> repository, final Function<T, T> preprocessor) {
    repository.findAll()
        .forEach(item -> {
          try {
            repository.delete(item);
            repository.save(preprocessor.apply(item));
          } catch (final Exception e) {
            log.error("Exception caught", e);
          }
        });
  }

  public void reindexRepositories() {
    // Redis-based repositories
    reindexRepository(shiftRepository, Function.identity());
    reindexRepository(tokensRepository, token -> token.id(null));
    reindexRepository(tripRepository, tripUtils::setZones);

    // Mongo-based repositories
    reindexRepository(driverRepository, Function.identity());
    reindexRepository(driverReviewRepository, Function.identity());
    reindexRepository(groupRepository, Function.identity());
    reindexRepository(placeRepository, Function.identity());
    reindexRepository(riderReviewRepository, Function.identity());
    reindexRepository(shiftArchiveRepository, Function.identity());
    reindexRepository(tripArchiveRepository, Function.identity());
    reindexRepository(userRepository, Function.identity());
    reindexRepository(vehicleRepository, Function.identity());
    reindexRepository(zoneRepository, Function.identity());
  }

  public void archiveShiftsAndTrips(final OffsetDateTime time) {
    StreamUtils.streamIterable(shiftRepository.findAll())
        .filter(shift -> time.compareTo(ObjectUtils.getOrDefault(shift.getEndTime(), OffsetDateTime.now())) >= 0)
        .forEach(shift -> {
          shiftArchiveRepository.save(shift);
          shiftRepository.delete(shift);
        });

    StreamUtils.streamIterable(tripRepository.findAll())
        .filter(trip -> (time.compareTo(tripUtils.getPrimaryTimeConstraint(trip)) >= 0))
        .forEach(trip -> {
          tripArchiveRepository.save(trip);
          tripRepository.delete(trip);
        });
  }
}
