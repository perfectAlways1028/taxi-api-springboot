package com.rubyride.tripmanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.session.web.socket.events.SessionConnectEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {
  private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

  private static String getUserName(final Message<byte[]> message) {
    final var headerAccessor = StompHeaderAccessor.wrap(message);
    final var authToken = headerAccessor.getHeader("simpUser");

    return authToken != null ?
        ((Authentication) authToken).getPrincipal().toString() :
        null;
  }

  @EventListener
  public void handlerWebSocketConnectListener(final SessionConnectEvent event) {
    final var userName = event.getWebSocketSession().getPrincipal() != null ?
        event.getWebSocketSession().getPrincipal().toString() :
        null;

    logger.info(userName != null ?
        "WebSocket User Connecting: " + userName :
        "WebSocket Connecting");
  }

  @EventListener
  public void handleWebSocketConnectedListener(final SessionConnectedEvent event) {
    final var userName = getUserName(event.getMessage());

    logger.info(userName != null ?
        "WebSocket User Connected: " + userName :
        "WebSocket Connected");
  }

  @EventListener
  public void handleWebSocketDisconnectListener(final SessionDisconnectEvent event) {
    final var userName = getUserName(event.getMessage());

    logger.info(userName != null ?
        "WebSocket User Disconnected: " + userName :
        "WebSocket Disconnected");
  }
}