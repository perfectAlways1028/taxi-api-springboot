package com.rubyride.tripmanager.service;

import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.JsonParser;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.turf.TurfJoins;
import com.rubyride.model.LatitudeLongitude;
import com.rubyride.model.Place;
import com.rubyride.model.Zone;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.utility.GeoUtils;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MapService {
  private static final Logger log = LoggerFactory.getLogger(MapService.class);

  private final GeoUtils geoUtils;
  private final PlaceRepository placeRepository;
  private final ZoneRepository zoneRepository;

  // Rate limiter so as to not violate OSM's TOS
  private final RateLimiter rateLimiter = RateLimiter.create(1D);

  @Autowired
  public MapService(final GeoUtils geoUtils, final PlaceRepository placeRepository, final ZoneRepository zoneRepository) {
    this.geoUtils = geoUtils;
    this.placeRepository = placeRepository;
    this.zoneRepository = zoneRepository;
  }

  /**
   * Set latitude, longitude, and zone ID on a {@link Place}
   *
   * @param place Place to geocode
   * @return Place with geocoding performed on it
   */
  public Place geocode(final Place place) {
    final var address = place.getAddress();

    if (address != null) {
      final var httpClient = HttpClient.newBuilder()
          .version(HttpClient.Version.HTTP_1_1)
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofMillis(2_000L))
          .build();

      try {
        final var uriBuilder = new URIBuilder()
            .setScheme("https")
            .setHost("nominatim.openstreetmap.org")
            .setPath("search")
            .addParameter("format", "jsonv2")
            .addParameter("limit", "1");

        if (address.getStreet() != null) {
          uriBuilder.addParameter("street", address.getStreet());
        }

        if (address.getCity() != null) {
          uriBuilder.addParameter("city", address.getCity());
        }

        if (address.getState() != null) {
          uriBuilder.addParameter("state", address.getState());
        }

        if (address.getZipcode() != null) {
          uriBuilder.addParameter("postalcode", address.getZipcode());
        }

        rateLimiter.acquire();

        final var httpRequest = HttpRequest.newBuilder()
            .uri(uriBuilder.build())
            .header(HttpHeaders.USER_AGENT, "RubyRide Place Geocoder/1.0")
            .build();

        final var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == HttpStatus.SC_OK) {
          final var responseArray = JsonParser.parseString(response.body())
              .getAsJsonArray();

          if (responseArray.size() > 0) {
            final var jsonObject = responseArray
                .get(0)
                .getAsJsonObject();

            final var location = new LatitudeLongitude()
                .latitude(jsonObject.get("lat")
                    .getAsDouble())
                .longitude(jsonObject.get("lon")
                    .getAsDouble());

            place.setLocation(location);

            // Set zone for place if possible
            zoneRepository.findAll().stream()
                .filter(zone -> {
                  final var geoPolygon = geoUtils.getPolygonFromString(zone.getId(), zone.getBounds());
                  if (geoPolygon == null) {
                    return false;
                  }

                  final var turfPolygon = Polygon.fromLngLats(
                      List.of(StreamUtils.streamIterator(geoPolygon.iterator())
                          .map(point -> Point.fromLngLat(point.getX(), point.getY()))
                          .collect(Collectors.toList())));

                  return TurfJoins.inside(Point.fromLngLat(location.getLongitude(), location.getLatitude()), turfPolygon);
                })
                .findAny()
                .map(Zone::getId)
                .ifPresent(place::setZoneId);
          }
        }
      } catch (final IOException |
          InterruptedException |
          URISyntaxException e) {
        log.error("Exception caught", e);
      }
    }

    return place;
  }

  @Scheduled(cron = "0 0 3 1/1 * ?")
  public void geocodePlacesWithoutLocation() {
    placeRepository.findByLocationIsNullOrLocationLatitudeIsNullOrLocationLongitudeIsNull().stream()
        .map(this::geocode)
        .filter(Objects::nonNull)
        .forEach(placeRepository::save);
  }
}
