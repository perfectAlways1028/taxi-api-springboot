package com.rubyride.tripmanager.security;

import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserTokens implements Serializable {
  @Id
  private UUID id;
  private UUID userId;
  private Set<String> savedTokens;

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(final UUID userId) {
    this.userId = userId;
  }

  public UUID getId() {
    return id;
  }

  public void setId(final UUID id) {
    this.id = id;
  }

  public UserTokens id(final UUID id) {
    this.id = id;
    return this;
  }

  public UserTokens userId(final UUID userId) {
    this.userId = userId;
    return this;
  }

  public UserTokens saveToken(final String token) {
    getSavedTokens().add(token);
    return this;
  }

  public UserTokens replaceToken(final String oldToken, final String newToken) {
    getSavedTokens().remove(oldToken);
    getSavedTokens().add(newToken);
    return this;
  }

  public UserTokens removeToken(final String token) {
    getSavedTokens().remove(token);
    return this;
  }

  public final Set<String> getSavedTokens() {
    if (savedTokens == null) {
      savedTokens = new HashSet<>();
    }
    return savedTokens;
  }
}
