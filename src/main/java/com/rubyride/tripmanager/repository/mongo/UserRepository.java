package com.rubyride.tripmanager.repository.mongo;

import com.rubyride.model.Role;
import com.rubyride.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends MongoRepository<User, UUID> {
  User findByUserName(String userName);

  User findByEmail(String email);

  List<User> findAllByRolesContaining(Role role);

  List<User> findAllByZonesContaining(UUID zoneId);

  List<User> findAllByZonesContainingAndRolesContaining(UUID zoneId, Role role);

  List<User> findAllByPartnerId(UUID partnerId);
}
