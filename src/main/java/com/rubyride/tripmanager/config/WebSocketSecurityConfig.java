package com.rubyride.tripmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
  @Override
  protected boolean sameOriginDisabled() {
    return true;
  }

  @Override
  protected void configureInbound(final MessageSecurityMetadataSourceRegistry messages) {
    messages
        .simpSubscribeDestMatchers("/topic/driverLocations").access("@accessControl.canAccessLocations()")
        .simpSubscribeDestMatchers("/topic/driverLocation/{driverId:[0-9a-fA-F\\-]+}").access("@accessControl.canAccessLocation(#driverId)")
        .simpSubscribeDestMatchers("/topic/processingExceptions").access("@accessControl.canAccessTripSchedulingExceptions()")
        .simpSubscribeDestMatchers("/topic/shifts").access("@accessControl.canAccessShifts()")
        .simpSubscribeDestMatchers("/topic/shifts/{shiftId:[0-9a-fA-F\\-]+}").access("@accessControl.canAccessShift(#shiftId)")
        .simpSubscribeDestMatchers("/topic/trips").access("@accessControl.canAccessTripRequests()")
        .simpSubscribeDestMatchers("/topic/trips/{tripId:[0-9a-fA-F\\-]+}").access("@accessControl.canAccessTripRequest(#tripId)");
  }
}
