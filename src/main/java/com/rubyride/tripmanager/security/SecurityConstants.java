package com.rubyride.tripmanager.security;

public final class SecurityConstants {
  public static final String SECRET = "SecretKeyToGenJWTs";
  public static final long EXPIRATION_TIME = 864_000_000L; // 10 days
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final String HEADER_STRING = "Authorization";
  public static final String ACTUATOR_URL = "/actuator/**";
  public static final String CONTACT_URL = "/contact**";
  public static final String USER_CREATION_URL = "/users";
  public static final String USER_EXISTS_URL = "/users/userExists/**";
  public static final String PASSWORD_RESET_URL = "/login/**";
  public static final String LOGOUT_URL = "/logout";
  public static final String WEBSOCKET_URL = "/websocket";
}
