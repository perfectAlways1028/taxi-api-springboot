package com.rubyride.tripmanager.security;

import com.rubyride.tripmanager.service.TwilioService;
import com.rubyride.tripmanager.utility.SpringContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JWTLogoutHandler implements LogoutHandler {
  private final TokenManager tokenManager;
  private final TwilioService twilioService;
  private final SpringContext springContext;

  public JWTLogoutHandler(final TokenManager tokenManager, final TwilioService twilioService, final SpringContext springContext) {
    this.tokenManager = tokenManager;
    this.twilioService = twilioService;
    this.springContext = springContext;
  }

  @Override
  public void logout(final HttpServletRequest request, final HttpServletResponse response, final Authentication authentication) {
    springContext.getAuthenticatedUserIdFromRequest(request)
        .ifPresent(userId -> {
          tokenManager.revokeTokens(userId);
          twilioService.removeBindingsForUserIdentity(userId);
        });
  }
}
