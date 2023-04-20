package com.rubyride.tripmanager.repository.redis;

import com.rubyride.tripmanager.security.UserTokens;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserTokensRepository extends KeyValueRepository<UserTokens, UUID> {
  Optional<UserTokens> findByUserId(UUID userId);
}
