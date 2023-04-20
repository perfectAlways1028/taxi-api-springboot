package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Partner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PartnerRepository extends MongoRepository<Partner, UUID> {
  Optional<Partner> findByName(String name);

  Optional<Partner> findByFacilitiesContains(UUID facilityId);
}
