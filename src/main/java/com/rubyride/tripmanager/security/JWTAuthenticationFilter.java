package com.rubyride.tripmanager.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rubyride.model.UserCredentials;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.service.TwilioService;
import com.rubyride.tripmanager.utility.SpringContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
  private final AuthenticationManager authenticationManager;
  private final SpringContext springContext;

  private ObjectMapper objectMapper;
  private UserRepository userRepository;
  private TokenManager tokenManager;
  private TwilioService twilioService;

  public JWTAuthenticationFilter(final AuthenticationManager authenticationManager, final SpringContext springContext) {
    this.authenticationManager = authenticationManager;
    this.springContext = springContext;
  }

  @Override
  public Authentication attemptAuthentication(final HttpServletRequest request,
                                              final HttpServletResponse response) throws AuthenticationException {
    try {
      final var userCredentials = new ObjectMapper()
          .readValue(request.getInputStream(), UserCredentials.class);

      final var authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              userCredentials.getUserName(),
              userCredentials.getPassword(),
              new ArrayList<>())
      );

      if (authentication.isAuthenticated()) {
        var user = getUserRepository().findByUserName(userCredentials.getUserName());
        if (user == null) {
          // try again with e-mail
          user = getUserRepository().findByEmail(userCredentials.getUserName());
        }

        if (userCredentials.getApn() != null) {
          getTwilioService().addApnToUserIdentity(user.getId(), userCredentials.getApn());
        }

        if (userCredentials.getFcm() != null) {
          getTwilioService().addFcmToUserIdentity(user.getId(), userCredentials.getFcm());
        }
      }

      return authentication;
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private TwilioService getTwilioService() {
    if (twilioService != null) {
      return twilioService;
    }

    twilioService = springContext.getBean(TwilioService.class);
    return twilioService;
  }

  private UserRepository getUserRepository() {
    if (userRepository != null) {
      return userRepository;
    }

    userRepository = springContext.getBean(UserRepository.class);
    return userRepository;
  }

  private TokenManager getTokenManager() {
    if (tokenManager != null) {
      return tokenManager;
    }

    tokenManager = springContext.getBean(TokenManager.class);
    return tokenManager;
  }

  private ObjectMapper getObjectMapper() {
    if (objectMapper != null) {
      return objectMapper;
    }

    objectMapper = springContext.getBean(ObjectMapper.class);
    return objectMapper;
  }

  @Override
  protected void successfulAuthentication(final HttpServletRequest request,
                                          final HttpServletResponse response,
                                          final FilterChain chain,
                                          final Authentication authentication) throws IOException {
    final var username = ((org.springframework.security.core.userdetails.User) authentication.getPrincipal()).getUsername();
    final var user = getUserRepository().findByUserName(username);

    // scrub password before returning
    user.setPassword(null);

    final var token = JWT.create()
        .withSubject(username)
        .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
        .sign(Algorithm.HMAC512(SecurityConstants.SECRET.getBytes()));

    getTokenManager().saveToken(token);

    response.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
    response.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    var exposeHeaders = response.getHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
    if (exposeHeaders != null) {
      exposeHeaders += ", " + SecurityConstants.HEADER_STRING;
    } else {
      exposeHeaders = SecurityConstants.HEADER_STRING;
    }

    response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);

    response.getWriter().write(getObjectMapper().writeValueAsString(user));
  }
}
