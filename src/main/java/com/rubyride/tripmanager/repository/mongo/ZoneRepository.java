package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Zone;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ZoneRepository extends MongoRepository<Zone, UUID> {
}
