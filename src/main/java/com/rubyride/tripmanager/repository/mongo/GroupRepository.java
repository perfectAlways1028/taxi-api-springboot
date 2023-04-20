package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Group;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupRepository extends MongoRepository<Group, UUID> {
  List<Group> findByOriginZoneId(UUID zoneId);

  List<Group> findByParentGroupId(UUID parentGroupId);
}
