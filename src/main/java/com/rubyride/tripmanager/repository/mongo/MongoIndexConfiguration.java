package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.*;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import javax.annotation.PostConstruct;

@Configuration
public class MongoIndexConfiguration {
  private final MongoOperations mongoOperations;

  @Autowired
  public MongoIndexConfiguration(final MongoOperations mongoOperations) {
    this.mongoOperations = mongoOperations;
  }

  private <T> IndexOperations buildIndexes(final Class<T> clazz, final String... columns) {
    final var indices = mongoOperations.indexOps(clazz);

    StreamUtils.safeStream(columns)
        .map(column -> new Index(column, Sort.DEFAULT_DIRECTION))
        .forEach(indices::ensureIndex);

    return indices;
  }

  @PostConstruct
  public void configureIndexes() {
    buildIndexes(Driver.class, "userId", "homeZone", "assignedZone");
    buildIndexes(TripReview.class, "driverId", "riderId", "tripId");
    buildIndexes(Group.class, "parentGroupId", "originZoneId");

    final var placeIndexes = buildIndexes
        (Place.class, "groups", "zoneId");
    placeIndexes.ensureIndex(new CompoundIndexDefinition(new Document()
        .append("id", 1)
        .append("zoneId", 1)));
    placeIndexes.ensureIndex(new GeospatialIndex("location"));

    buildIndexes(Shift.class, "driverId");
    buildIndexes(TripRequest.class, "fromZoneId", "toZoneId", "riderId");
    buildIndexes(User.class, "zones", "roles", "email", "userName");

    buildIndexes(Vehicle.class);

    final var zoneIndexes = buildIndexes(Zone.class);
    zoneIndexes.ensureIndex(new GeospatialIndex("bounds"));

    buildIndexes(Partner.class, "name", "facilities");
    buildIndexes(PartnerTransportationRequest.class, "facility");

    buildIndexes(TripSchedulingException.class, "time");

    buildIndexes(DataBlob.class, "references");
  }
}
