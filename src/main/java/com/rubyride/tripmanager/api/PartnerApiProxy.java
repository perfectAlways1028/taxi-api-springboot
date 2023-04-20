package com.rubyride.tripmanager.api;

import com.rubyride.api.PartnerApi;
import com.rubyride.model.Partner;
import com.rubyride.model.Place;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@RestController
public class PartnerApiProxy implements PartnerApi {
  private final PartnerApiImpl partnerApi;

  @Autowired
  public PartnerApiProxy(final PartnerApiImpl partnerApi) {
    this.partnerApi = partnerApi;
  }

  @Override
  public ResponseEntity<Partner> addFacilityToPartner(final UUID partnerId, @NotNull @Valid final UUID facilityId) {
    return partnerApi.addFacilityToPartner(partnerId, facilityId);
  }

  @Override
  public ResponseEntity<Partner> addPartner(@Valid final Partner partner) {
    return partnerApi.addPartner(partner);
  }

  @Override
  public ResponseEntity<Partner> deleteFacilityFromPartner(final UUID partnerId, final UUID facilityId) {
    return partnerApi.deleteFacilityFromPartner(partnerId, facilityId);
  }

  @Override
  public ResponseEntity<Void> deletePartner(final UUID partnerId) {
    return partnerApi.deletePartner(partnerId);
  }

  @Override
  public ResponseEntity<List<Place>> getFacilitiesForPartner(final UUID partnerId) {
    return partnerApi.getFacilitiesForPartner(partnerId);
  }

  @Override
  public ResponseEntity<Partner> getPartnerById(final UUID partnerId) {
    return partnerApi.getPartnerById(partnerId);
  }

  @Override
  public ResponseEntity<List<Partner>> getPartners() {
    return partnerApi.getPartners();
  }

  @Override
  public ResponseEntity<Partner> updatePartner(@Valid final Partner partner) {
    return partnerApi.updatePartner(partner);
  }
}
