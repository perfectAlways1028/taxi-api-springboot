package com.rubyride.tripmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

@Configuration
public class MessageSourceConfig {
  @Bean
  public ResourceBundleMessageSource messageSource() {
    final var source = new ResourceBundleMessageSource();
    source.setBasenames("messages/messages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());

    return source;
  }
}
