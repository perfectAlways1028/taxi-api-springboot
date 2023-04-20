package com.rubyride.tripmanager.api;

import com.rubyride.api.PartnerTransportationRequestApi;
import com.rubyride.model.PartnerTransportationRequest;
import com.rubyride.model.TimeConstraint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
public class PartnerTransportationRequestProxy implements PartnerTransportationRequestApi {
  private final PartnerTransportationRequestApiImpl partnerTransportationRequestApi;

  @Autowired
  public PartnerTransportationRequestProxy(final PartnerTransportationRequestApiImpl partnerTransportationRequestApi) {
    this.partnerTransportationRequestApi = partnerTransportationRequestApi;
  }

  @Override
  public ResponseEntity<Void> deletePartnerTransportationRequest(final UUID transportationRequestId) {
    return partnerTransportationRequestApi.deletePartnerTransportationRequest(transportationRequestId);
  }

  @Override
  public ResponseEntity<List<PartnerTransportationRequest>> getPartnerTransportationRequest(final List<UUID> transportationRequestId) {
    return partnerTransportationRequestApi.getPartnerTransportationRequest(transportationRequestId);
  }

  @Override
  public ResponseEntity<List<PartnerTransportationRequest>> getPartnerTransportationRequestByPartner(final UUID partnerId) {
    return partnerTransportationRequestApi.getPartnerTransportationRequestByPartner(partnerId);
  }

  @Override
  public ResponseEntity<PartnerTransportationRequest> requestPartnerTransportationRequest(@Valid final PartnerTransportationRequest partnerTransportationRequest) {
    return partnerTransportationRequestApi.requestPartnerTransportationRequest(partnerTransportationRequest);
  }

  @Override
  public ResponseEntity<PartnerTransportationRequest> scheduleTripFromPartnerTransportationRequest(final UUID transportationRequestId, @Valid final TimeConstraint timeConstraint) {
    return partnerTransportationRequestApi.scheduleTripFromPartnerTransportationRequest(transportationRequestId, timeConstraint);
  }

  @Override
  public ResponseEntity<PartnerTransportationRequest> updatePartnerTransportationRequest(@Valid final PartnerTransportationRequest partnerTransportationRequest) {
    return partnerTransportationRequestApi.updatePartnerTransportationRequest(partnerTransportationRequest);
  }
}
