package com.rubyride.tripmanager;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@TestConfiguration
public class TestMongoConfiguration {
  @Value("${MONGODB_HOST}")
  private String host;

  @Value("${MONGODB_PORT}")
  private int port;

  private MongodExecutable mongodExecutable;

  @PreDestroy
  void clean() {
    if(!SystemUtils.IS_OS_WINDOWS) {
      mongodExecutable.stop();
    }
  }

  @PostConstruct
  void setup() throws Exception {
    if(!SystemUtils.IS_OS_WINDOWS) {
      final IMongodConfig mongodConfig = new MongodConfigBuilder()
              .version(Version.Main.PRODUCTION)
              .net(new Net(host, port, Network.localhostIsIPv6()))
              .build();

      final MongodStarter starter = MongodStarter.getDefaultInstance();
      mongodExecutable = starter.prepare(mongodConfig);
      mongodExecutable.start();
    }
  }
}
