package com.rubyride.tripmanager.api;

import com.rubyride.model.Partner;
import com.rubyride.model.Place;
import com.rubyride.tripmanager.exception.EntityAlreadyExistsException;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.PartnerRepository;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PartnerApiImpl {
  private final PartnerRepository partnerRepository;
  private final PlaceRepository placeRepository;

  @Autowired
  public PartnerApiImpl(final PartnerRepository partnerRepository, final PlaceRepository placeRepository) {
    this.partnerRepository = partnerRepository;
    this.placeRepository = placeRepository;
  }

  private Partner findPartner(final UUID partnerId) {
    return partnerRepository.findById(partnerId)
        .orElseThrow(() -> new EntityNotFoundException("Partner not found"));
  }

  @PreAuthorize("@accessControl.canWritePartner(#partnerId)")
  public ResponseEntity<Partner> addFacilityToPartner(final UUID partnerId, @NotNull @Valid final UUID facilityId) {
    return updatePartner(findPartner(partnerId).addFacilitiesItem(facilityId));
  }

  @PreAuthorize("@accessControl.canAccessPartners()")
  public ResponseEntity<Partner> addPartner(@Valid final Partner partner) {
    final var existingPartner = partnerRepository.findByName(partner.getName());
    if (existingPartner.isPresent()) {
      throw new EntityAlreadyExistsException("Partner already exists");
    }

    partner.setId(UUID.randomUUID());

    partnerRepository.insert(partner);

    return ResponseEntity.created(URI.create("/v1/partners/" + partner.getId()))
        .body(partner);
  }

  @PreAuthorize("@accessControl.canWritePartner(#partnerId)")
  public ResponseEntity<Partner> deleteFacilityFromPartner(final UUID partnerId, final UUID facilityId) {
    final var partner = findPartner(partnerId);

    final var facilities = partner.getFacilities();
    facilities.remove(facilityId);

    return updatePartner(partner.facilities(facilities));
  }

  @PreAuthorize("@accessControl.canAccessPartners()")
  public ResponseEntity<Void> deletePartner(final UUID partnerId) {
    partnerRepository.deleteById(partnerId);

    return ResponseEntity.noContent()
        .build();
  }

  @PreAuthorize("@accessControl.canReadPartner(#partnerId)")
  public ResponseEntity<List<Place>> getFacilitiesForPartner(final UUID partnerId) {
    final var partner = findPartner(partnerId);

    return ResponseEntity.ok()
        .body(StreamUtils.safeStream(partner.getFacilities())
            .map(placeRepository::findById)
            .flatMap(Optional::stream)
            .collect(Collectors.toList()));
  }

  @PreAuthorize("@accessControl.canReadPartner(#partnerId)")
  public ResponseEntity<Partner> getPartnerById(final UUID partnerId) {
    return ResponseEntity.of(partnerRepository.findById(partnerId));
  }

  @PreAuthorize("@accessControl.canAccessPartners()")
  public ResponseEntity<List<Partner>> getPartners() {
    return ResponseEntity.ok(partnerRepository.findAll());
  }

  @PreAuthorize("@accessControl.canWritePartner(#partner.id)")
  public ResponseEntity<Partner> updatePartner(@Valid final Partner partner) {
    return partnerRepository.findById(partner.getId())
        .map(existingPartner -> {
          if (partner.getName() != null) {
            existingPartner.setName(partner.getName());
          }

          if (partner.getFacilities() != null) {
            existingPartner.setFacilities(partner.getFacilities());
          }

          partnerRepository.save(existingPartner);

          return ResponseEntity.ok()
              .location(URI.create("/v1/partners/" + existingPartner.getId()))
              .body(existingPartner);
        })
        .orElseThrow(() -> new EntityNotFoundException("Partner not found"));
  }
}
