package com.rubyride.tripmanager.repository.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.DBObject;
import com.mongodb.MongoClientSettings;
import com.rubyride.model.LatitudeLongitude;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.geo.Point;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.mapping.MongoSimpleTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

@Configuration
public class MongoRepositoryConfiguration extends AbstractMongoClientConfiguration {
  @Value("${MONGODB_AUTH}")
  private boolean auth;

  @Override
  protected String getDatabaseName() {
    return System.getenv().getOrDefault("MONGODB_NAME", "trip-manager");
  }

  @Override
  public void configureClientSettings(final MongoClientSettings.Builder builder) {
    final var host = System.getenv().getOrDefault("MONGODB_HOST", "localhost");
    final var port = Integer.parseInt(System.getenv().getOrDefault("MONGODB_PORT", "27017"));
    final var username = System.getenv().getOrDefault("MONGODB_USER", "mongo");
    final var password = System.getenv().getOrDefault("MONGODB_PASSWORD", "mongo");
    final var connectionString = "mongodb://" + (!Boolean.FALSE.equals(auth) ?
        (username + ":" + password + "@") :
        "") + host + ":" + port;

    builder.applyConnectionString(new ConnectionString(connectionString));
  }

  @Override
  protected boolean autoIndexCreation() {
    return true;
  }

  @Override
  public MongoMappingContext mongoMappingContext(final MongoCustomConversions customConversions) throws ClassNotFoundException {
    final var context = super.mongoMappingContext(customConversions);
    context.setSimpleTypeHolder(new SimpleTypeHolder(Set.of(OffsetDateTime.class), MongoSimpleTypes.HOLDER));
    context.afterPropertiesSet();
    return context;
  }

  @Override
  public MongoCustomConversions customConversions() {
    final var converterList = new ArrayList<Converter<?, ?>>();
    converterList.add(new Converter<Date, OffsetDateTime>() {
      @Override
      public OffsetDateTime convert(final Date date) {
        return date.toInstant().atOffset(ZoneOffset.UTC);
      }
    });
    converterList.add(new Converter<OffsetDateTime, Date>() {
      @Override
      public Date convert(final OffsetDateTime offsetDateTime) {
        return Date.from(offsetDateTime.toInstant());
      }
    });
    converterList.add(new Converter<String, OffsetDateTime>() {
      @Override
      public OffsetDateTime convert(final String date) {
        return OffsetDateTime.parse(date);
      }
    });
    converterList.add(new Converter<OffsetDateTime, String>() {
      @Override
      public String convert(final OffsetDateTime date) {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(date);
      }
    });

    converterList.add(new Converter<Point, LatitudeLongitude>() {
      @Override
      public LatitudeLongitude convert(final Point point) {
        return new LatitudeLongitude()
            .latitude(point.getY())
            .longitude(point.getX());
      }
    });
    converterList.add(new Converter<LatitudeLongitude, Point>() {
      @Override
      public Point convert(final LatitudeLongitude latLong) {
        return new Point(latLong.getLongitude(), latLong.getLatitude());
      }
    });

    converterList.add(new Converter<Point, DBObject>() {
      @Override
      public DBObject convert(final Point point) {
        return new BasicDBObject()
            .append("type", "Point")
            .append("coordinates", new Double[]{point.getX(), point.getY()});
      }
    });
    converterList.add(new Converter<DBObject, Point>() {
      @Override
      public Point convert(final DBObject source) {
        final double[] coordinates = (double[]) source.get("coordinates");
        return new Point(coordinates[0], coordinates[1]);
      }
    });

    return new MongoCustomConversions(converterList);
  }
}
