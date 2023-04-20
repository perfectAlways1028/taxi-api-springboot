package com.rubyride.tripmanager.repository.redis;

import com.rubyride.model.Shift;
import com.rubyride.model.TripRequest;
import com.rubyride.tripmanager.security.UserTokens;
import org.springframework.data.redis.core.index.IndexConfiguration;
import org.springframework.data.redis.core.index.IndexDefinition;
import org.springframework.data.redis.core.index.SimpleIndexDefinition;

import java.util.List;

public class SecondaryIndexConfiguration extends IndexConfiguration {
  public SecondaryIndexConfiguration() {
    super();
  }

  @Override
  protected Iterable<IndexDefinition> initialConfiguration() {
    return List.of(
        new SimpleIndexDefinition(TripRequest.class.getName(), "riderId"),
        new SimpleIndexDefinition(TripRequest.class.getName(), "fromZoneId"),
        new SimpleIndexDefinition(TripRequest.class.getName(), "toZoneId"),

        new SimpleIndexDefinition(Shift.class.getName(), "driverId"),
        new SimpleIndexDefinition(Shift.class.getName(), "active"),
        new SimpleIndexDefinition(Shift.class.getName(), "zoneId"),

        new SimpleIndexDefinition(UserTokens.class.getName(), "userId"));
  }
}
