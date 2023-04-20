package com.rubyride.tripmanager.config;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

public class CustomWebSocketHandlerDecorator extends WebSocketHandlerDecorator {
  public CustomWebSocketHandlerDecorator(final WebSocketHandler delegate) {
    super(delegate);
  }


  @Override
  public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
    if (message instanceof TextMessage) {
      final TextMessage msg = (TextMessage) message;
      final String payload = msg.getPayload();

      // only add \00 if not present (iOS / Android)
      if (!payload.endsWith("\u0000") && !payload.equals("\n")) {
        super.handleMessage(session, new TextMessage(payload + "\u0000"));
      } else {
        super.handleMessage(session, message);
      }
    } else {
      super.handleMessage(session, message);
    }
  }
}