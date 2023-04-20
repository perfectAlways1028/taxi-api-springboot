package com.rubyride.tripmanager.security;

import com.rubyride.tripmanager.utility.SpringContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {
  private final SpringContext springContext;

  private TokenAuthentication tokenAuthentication;

  public JWTAuthorizationFilter(final AuthenticationManager authManager, final SpringContext springContext) {
    super(authManager);
    this.springContext = springContext;
  }

  @Override
  protected void doFilterInternal(final HttpServletRequest request,
                                  final HttpServletResponse response,
                                  final FilterChain chain) throws IOException, ServletException {
    final var header = request.getHeader(SecurityConstants.HEADER_STRING);

    if (header != null && header.startsWith(SecurityConstants.TOKEN_PREFIX)) {
      SecurityContextHolder.getContext().setAuthentication(getAuthentication(request));
    }

    chain.doFilter(request, response);
  }

  private TokenAuthentication getTokenAuthentication() {
    if (tokenAuthentication != null) {
      return tokenAuthentication;
    }

    tokenAuthentication = springContext.getBean(TokenAuthentication.class);
    return tokenAuthentication;
  }

  private UsernamePasswordAuthenticationToken getAuthentication(final HttpServletRequest request) {
    final var headerToken = request.getHeader(SecurityConstants.HEADER_STRING);

    return getTokenAuthentication().getAuthentication(headerToken);
  }
}
