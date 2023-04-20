package com.rubyride.tripmanager.repository.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.convert.RedisCustomConversions;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(RedisProperties.class)
@EnableRedisRepositories(indexConfiguration = SecondaryIndexConfiguration.class)
public class RedisConfiguration {
  @Value("${REDIS_HOST}")
  private String redisHost;

  @Value("${REDIS_PORT}")
  private int redisPort;

  @Value("${REDIS_PASSWORD}")
  private String redisPassword;

  public String getRedisHost() {
    return redisHost;
  }

  public int getRedisPort() {
    return redisPort;
  }

  public String getRedisPassword() {
    return redisPassword;
  }

  @Bean(destroyMethod = "shutdown")
  public ClientResources clientResources() {
    return DefaultClientResources.create();
  }

  @Bean
  public RedisStandaloneConfiguration redisStandaloneConfiguration() {
    final var config = new RedisStandaloneConfiguration(redisHost, redisPort);

    if (redisPassword != null && !redisPassword.isEmpty()) {
      config.setPassword(RedisPassword.of(redisPassword));
    }

    return config;
  }

  @Bean
  public ClientOptions clientOptions() {
    return ClientOptions.builder()
        .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
        .autoReconnect(true)
        .build();
  }

  @Bean
  public LettucePoolingClientConfiguration lettucePoolConfig(final ClientOptions options, @Qualifier("clientResources") final ClientResources dcr) {
    return LettucePoolingClientConfiguration.builder()
        .poolConfig(new GenericObjectPoolConfig<>())
        .clientOptions(options)
        .clientResources(dcr)
        .build();
  }

  @Bean
  public LettuceConnectionFactory redisConnectionFactory(final RedisStandaloneConfiguration redisStandaloneConfiguration,
                                                         final LettucePoolingClientConfiguration lettucePoolConfig) {
    return new LettuceConnectionFactory(redisStandaloneConfiguration, lettucePoolConfig);
  }

  @Bean
  @Primary
  public RedisTemplate<String, Object> redisTemplate(final ObjectMapper objectMapper, final RedisConnectionFactory redisConnectionFactory) {
    final var template = new RedisTemplate<String, Object>();
    template.setConnectionFactory(redisConnectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

    return template;
  }

  @Bean
  public RedisCustomConversions redisCustomConversions(final OffsetDateTimeToBytesConverter offsetToBytes,
                                                       final BytesToOffsetDateTimeConverter bytesToOffset) {
    return new RedisCustomConversions(List.of(offsetToBytes, bytesToOffset));
  }
}
