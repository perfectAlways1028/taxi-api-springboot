package com.rubyride.tripmanager.config;

import com.rubyride.tripmanager.security.TokenManager;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
  private final TokenManager tokenManager;

  @Autowired
  public WebSocketHandshakeInterceptor(final TokenManager tokenManager) {
    this.tokenManager = tokenManager;
  }

  private static Map<String, List<String>> splitQuery(final String url) {
    if (!StringUtils.hasText(url)) {
      return Collections.emptyMap();
    }
    return Arrays.stream(url.split("&"))
        .map(WebSocketHandshakeInterceptor::splitQueryParameter)
        .collect(Collectors.groupingBy(AbstractMap.SimpleImmutableEntry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
  }

  private static AbstractMap.SimpleImmutableEntry<String, String> splitQueryParameter(final String it) {
    final int idx = it.indexOf("=");
    final String key = idx > 0 ?
        it.substring(0, idx) :
        it;
    final String value = idx > 0 && it.length() > idx + 1 ? it.substring(idx + 1) : null;
    return new AbstractMap.SimpleImmutableEntry<>(key, value);
  }

  @Override
  public boolean beforeHandshake(final ServerHttpRequest request, final ServerHttpResponse response, final WebSocketHandler wsHandler, final Map<String, Object> attributes) {
    return StreamUtils.safeStream(splitQuery(request.getURI().getQuery()).get("token"))
        .anyMatch(token -> {
          if (!tokenManager.isValid(token)) {
            return false;
          }

          attributes.put("token", token);
          return true;
        });
  }

  @Override
  public void afterHandshake(final ServerHttpRequest request, final ServerHttpResponse response, final WebSocketHandler wsHandler, final Exception exception) {
  }
}
