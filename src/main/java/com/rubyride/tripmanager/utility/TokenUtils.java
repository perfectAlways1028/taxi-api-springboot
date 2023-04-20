package com.rubyride.tripmanager.utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.rubyride.tripmanager.security.SecurityConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TokenUtils {
  private static final Logger log = LoggerFactory.getLogger(TokenUtils.class);

  private TokenUtils() {
  }

  public static String getUserNameFromToken(final String token) {
    if (token == null) {
      return null;
    }

    try {
      return JWT.require(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()))
          .build()
          .verify(token.replace(SecurityConstants.TOKEN_PREFIX, ""))
          .getSubject();
    } catch (final JWTDecodeException e) {
      log.error("JWTDecodeException caught", e);
      return "";
    }
  }
}
