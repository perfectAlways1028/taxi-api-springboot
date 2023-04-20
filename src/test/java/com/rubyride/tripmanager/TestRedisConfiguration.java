package com.rubyride.tripmanager;

import com.rubyride.tripmanager.repository.redis.RedisConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;
import redis.embedded.util.OSDetector;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@TestConfiguration
public class TestRedisConfiguration {
  private final RedisServer redisServer;

  public TestRedisConfiguration(final RedisConfiguration redisConfiguration) throws IOException {
    final var customProvider = RedisExecProvider.defaultProvider()
        .override(OS.MAC_OS_X, Architecture.x86_64, "build/resources/test/redis-server.app");

    this.redisServer = RedisServer.builder()
        .redisExecProvider(customProvider)
        .port(redisConfiguration.getRedisPort())
        .bind("127.0.0.1")
        .setting("daemonize no")
        .build();
  }

  @PostConstruct
  public void postConstruct() {
    if(OSDetector.getOS() != OS.WINDOWS){
      redisServer.start();
    }
  }

  @PreDestroy
  public void preDestroy() {
    if(OSDetector.getOS() != OS.WINDOWS){
      redisServer.stop();
    }
  }
}
