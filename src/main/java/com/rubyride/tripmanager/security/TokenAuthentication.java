package com.rubyride.tripmanager.security;

import com.rubyride.model.Role;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.utility.StreamUtils;
import com.rubyride.tripmanager.utility.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class TokenAuthentication {
  private final TokenManager tokenManager;
  private final UserRepository userRepository;

  @Autowired
  public TokenAuthentication(final TokenManager tokenManager, final UserRepository userRepository) {
    this.tokenManager = tokenManager;
    this.userRepository = userRepository;
  }

  public UsernamePasswordAuthenticationToken getAuthentication(final String token) {
    if (token != null) {
      final var headerUserName = TokenUtils.getUserNameFromToken(token);

      if (headerUserName != null) {
        final var user = userRepository.findByUserName(headerUserName);

        if (user != null && tokenManager.isValid(token)) {
          return new UsernamePasswordAuthenticationToken(headerUserName,
              null,
              StreamUtils.safeStream(
                  user.getRoles())
                  .map(Role::toString)
                  .map(SimpleGrantedAuthority::new)
                  .collect(Collectors.toList()));
        }
      }
    }

    return null;
  }
}
