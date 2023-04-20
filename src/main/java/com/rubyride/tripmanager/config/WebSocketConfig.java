package com.rubyride.tripmanager.config;

import com.rubyride.tripmanager.security.SecurityConstants;
import com.rubyride.tripmanager.security.TokenAuthentication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final TokenAuthentication tokenAuthentication;
  private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;

  public WebSocketConfig(final TokenAuthentication tokenAuthentication, final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor) {
    this.tokenAuthentication = tokenAuthentication;
    this.webSocketHandshakeInterceptor = webSocketHandshakeInterceptor;
  }

  @Override
  public void configureMessageBroker(final MessageBrokerRegistry config) {
    final var taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(1);
    taskScheduler.setThreadNamePrefix("wss-heartbeat-thread-");
    taskScheduler.initialize();

    config.enableSimpleBroker("/topic", "/queue")
        .setTaskScheduler(taskScheduler)
        .setHeartbeatValue(new long[]{10_000L, 10_000L});
    config.setApplicationDestinationPrefixes("/app");
  }

  @Override
  public void registerStompEndpoints(final StompEndpointRegistry registry) {
    registry.addEndpoint(SecurityConstants.WEBSOCKET_URL)
        .setAllowedOrigins("*")
        .addInterceptors(
            webSocketHandshakeInterceptor,
            new HttpSessionHandshakeInterceptor());
  }

  @Override
  public void configureWebSocketTransport(final WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(CustomWebSocketHandlerDecorator::new);
  }

  @Override
  public void configureClientInboundChannel(final ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
      @Override
      public Message<?> preSend(final Message<?> message, final MessageChannel channel) {
        final StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
          final var accessToken = message
              .getHeaders()
              .get("simpSessionAttributes", Map.class)
              .get("token").toString();

          accessor.setUser(tokenAuthentication.getAuthentication(accessToken));
        }

        return message;
      }
    });
  }
}
