package com.rubyride.tripmanager.security;

import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.rubyride.model.User;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.repository.redis.UserTokensRepository;
import com.rubyride.tripmanager.utility.StreamUtils;
import com.rubyride.tripmanager.utility.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class TokenManager {
  private static final Logger log = LoggerFactory.getLogger(TokenManager.class);

  private final UserRepository userRepository;
  private final UserTokensRepository tokenRepository;

  public TokenManager(final UserRepository userRepository, final UserTokensRepository tokenRepository) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
  }

  public boolean isValid(final String token) {
    final var strippedToken = token.replaceAll(SecurityConstants.TOKEN_PREFIX, "");

    try {
      final var userName = TokenUtils.getUserNameFromToken(strippedToken);

      if (userName != null) {
        final var user = userRepository.findByUserName(userName);

        if (user != null) {
          return tokenRepository.findByUserId(user.getId())
              .map(userTokens -> userTokens.getSavedTokens().contains(strippedToken))
              .orElse(false);
        }
      }
    } catch (final JWTDecodeException e) {
      log.error("JWTDecodeException caught", e);
      return false;
    } catch (final TokenExpiredException e) {
      log.error("TokenExpiredException caught", e);
      return false;
    }

    return false;
  }

  public void saveToken(final String token) {
    final var userName = TokenUtils.getUserNameFromToken(token);

    if (userName != null) {
      final var user = userRepository.findByUserName(userName);

      if (user != null) {
        tokenRepository.save(tokenRepository.findByUserId(user.getId())
            .orElse(new UserTokens()
                .userId(user.getId()))
            .saveToken(token));
      }
    }
  }

  public void revokeTokens(final UUID userId) {
    tokenRepository.findByUserId(userId)
        .map(UserTokens::getId)
        .ifPresent(tokenRepository::deleteById);
  }

  public void revokeTokens(final String userName) {
    Optional.ofNullable(userRepository.findByUserName(userName))
        .map(User::getId)
        .ifPresent(this::revokeTokens);
  }

  // TODO: Make use of this for refresh tokens? (likely will also be part of UserTokens object)
  public void replaceToken(final UUID userId, final String oldToken, final String newToken) {
    tokenRepository.findById(userId)
        .map(userTokens -> userTokens.replaceToken(oldToken, newToken))
        .ifPresent(tokenRepository::save);
  }

  @Scheduled(cron = "0 0 5 1/1 * ?")
  public void flushExpiredTokens() {
    log.info("Flushing expired JWTs from token repository");

    StreamUtils.streamIterable(tokenRepository.findAll())
        .forEach(userTokens -> {
          userTokens.getSavedTokens().retainAll(
              userTokens.getSavedTokens().stream()
                  .filter(this::isValid)
                  .collect(Collectors.toSet()));

          tokenRepository.save(userTokens);
        });
  }
}
