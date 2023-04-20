package com.rubyride.tripmanager.api;

import com.rubyride.api.ZoneApi;
import com.rubyride.model.Zone;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.ZoneRepository;
import com.rubyride.tripmanager.utility.GeoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
public class ZoneApiImpl implements ZoneApi {
  private final ZoneRepository zoneRepository;
  private final GeoUtils geoUtils;

  public ZoneApiImpl(final ZoneRepository zoneRepository, final GeoUtils geoUtils) {
    this.zoneRepository = zoneRepository;
    this.geoUtils = geoUtils;
  }

  @Override
  public ResponseEntity<Zone> addZone(@Valid final Zone zone) {
    zone.setId(UUID.randomUUID());

    zoneRepository.insert(zone);

    return ResponseEntity.created(URI.create("/v1/zones/" + zone.getId().toString()))
        .body(zone);
  }

  @Override
  public ResponseEntity<Void> deleteZone(final UUID zoneId) {
    zoneRepository.deleteById(zoneId);

    return ResponseEntity.noContent()
        .build();
  }

  @Override
  public ResponseEntity<Zone> getZoneById(final UUID zoneId) {
    return ResponseEntity.of(zoneRepository.findById(zoneId));
  }

  @Override
  public ResponseEntity<List<Zone>> getZones() {
    return ResponseEntity.ok(zoneRepository.findAll());
  }

  @Override
  public ResponseEntity<Zone> setZoneBounds(final UUID zoneId, @Valid final String bounds) {
    return zoneRepository.findById(zoneId)
        .map(existingZone -> updateZone(existingZone.bounds(geoUtils.getPolygonAsString(bounds))))
        .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
  }

  @Override
  public ResponseEntity<Zone> updateZone(@Valid final Zone zone) {
    return zoneRepository.findById(zone.getId())
        .map(existingZone -> {
          if (zone.getName() != null) {
            existingZone.setName(zone.getName());
          }

          if (zone.getMapScale() != null) {
            existingZone.setMapScale(zone.getMapScale());
          }

          if (zone.getMapCenter() != null) {
            existingZone.setMapCenter(zone.getMapCenter());
          }

          if (zone.getBounds() != null) {
            existingZone.setBounds(zone.getBounds());
          }

          if (zone.getTimeZone() != null) {
            existingZone.setTimeZone(zone.getTimeZone());
          }

          zoneRepository.save(existingZone);

          return ResponseEntity.ok()
              .location(URI.create("/v1/zones/" + existingZone.getId().toString()))
              .body(existingZone);
        })
        .orElseThrow(() -> new EntityNotFoundException("Zone not found"));
  }
}
