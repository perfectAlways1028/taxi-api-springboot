package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Place;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlaceRepository extends MongoRepository<Place, UUID> {
  List<Place> findByZoneId(UUID zoneId);

  List<Place> findAllByGroupsContaining(UUID groupId);

  List<Place> findByLocationWithin(GeoJsonPolygon polygon);

  List<Place> findByLocationIsNullOrLocationLatitudeIsNullOrLocationLongitudeIsNull();

  boolean existsByIdAndZoneId(UUID id, UUID zoneId);
}
