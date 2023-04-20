package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.PartnerTransportationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PartnerTransportationRequestRepository extends MongoRepository<PartnerTransportationRequest, UUID> {
  List<PartnerTransportationRequest> findByFacilityIdIn(Collection<UUID> facilityId);
}
