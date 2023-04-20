package com.rubyride.tripmanager.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.rubyride.tripmanager.exception.InvalidGeoJsonException;
import org.geojson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.GeoJsonPolygon;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GeoUtils {
  private static final Logger log = LoggerFactory.getLogger(GeoUtils.class);
  private final Cache<UUID, GeoJsonPolygon> polygonCache = CacheBuilder.newBuilder()
      .expireAfterWrite(Duration.ofHours(8L))
      .build();
  private final ObjectMapper objectMapper;

  public GeoUtils(final ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String getPolygonAsString(final String objectToParse) {
    try {
      return objectMapper.writeValueAsString(GeoJsonObjectType.convert(objectMapper.readValue(objectToParse, GeoJsonObject.class)).getPoints());
    } catch (final JsonProcessingException e) {
      log.error("JsonProcessingException caught", e);
      throw new InvalidGeoJsonException(e);
    }
  }

  public GeoJsonPolygon getPolygonFromString(final UUID zoneId, final String object) {
    if (object == null) {
      return null;
    }

    try {
      return polygonCache.get(zoneId, () -> {
        final var type = objectMapper.getTypeFactory()
            .constructCollectionType(List.class, Point.class);
        return new GeoJsonPolygon(objectMapper.readValue(object, type));
      });
    } catch (final ExecutionException e) {
      log.error("ExecutionException caught", e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unused")
  private enum GeoJsonObjectType {
    FEATURE_COLLECTION(FeatureCollection.class,
        object -> GeoJsonObjectType.getCoordinates(((FeatureCollection) object).getFeatures().get(0).getGeometry())),

    POLYGON(Polygon.class,
        object -> StreamUtils.safeStream(((Polygon) object).getExteriorRing())),

    LINE_STRING(LineString.class,
        object -> StreamUtils.safeStream(((LineString) object).getCoordinates())),

    NOT_SUPPORTED(GeoJsonObject.class,
        object -> {
          throw new UnsupportedGeoJsonException(
              "GeoJson object contains unsupported feature '" + object.getClass().getSimpleName() + "'");
        });

    private static final Map<Class<? extends GeoJsonObject>, GeoJsonObjectType> map =
        EnumSet.allOf(GeoJsonObjectType.class).stream()
            .collect(Collectors.toMap(value -> value.clazz, Function.identity()));

    private final Class<? extends GeoJsonObject> clazz;
    private final Function<GeoJsonObject, Stream<LngLatAlt>> converter;

    GeoJsonObjectType(final Class<? extends GeoJsonObject> clazz, final Function<GeoJsonObject, Stream<LngLatAlt>> converter) {
      this.clazz = clazz;
      this.converter = converter;
    }

    private static Stream<LngLatAlt> getCoordinates(final GeoJsonObject object) {
      return map.getOrDefault(object.getClass(), NOT_SUPPORTED).converter.apply(object);
    }

    private static GeoJsonPolygon convert(final GeoJsonObject object) {
      return new GeoJsonPolygon(getCoordinates(object)
          .map(lngLatAlt -> new Point(lngLatAlt.getLongitude(), lngLatAlt.getLatitude()))
          .collect(Collectors.toList()));
    }
  }

  private static final class UnsupportedGeoJsonException extends RuntimeException {
    private UnsupportedGeoJsonException(final String message) {
      super(message);
    }
  }
}
