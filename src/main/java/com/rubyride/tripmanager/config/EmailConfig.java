package com.rubyride.tripmanager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties
public class EmailConfig {
  @Value("${spring.mail.host:}")
  private String host;

  @Value("${spring.mail.port:}")
  private int port;

  @Value("${spring.mail.username:}")
  private String username;

  @Value("${spring.mail.password:}")
  private String password;

  @Bean
  public JavaMailSenderImpl emailServer() {
    final var server = new JavaMailSenderImpl();
    server.setHost(host);
    server.setPort(port);
    server.setUsername(username);
    server.setPassword(password);

    final var properties = new Properties();
    properties.put("mail.smtp.starttls.enable", "true");
    properties.put("mail.smtp.auth", "true");
    properties.put("mail.transport.protocol", "smtp");
//    properties.put("mail.debug", "true");

    server.setJavaMailProperties(properties);

    return server;
  }
}
