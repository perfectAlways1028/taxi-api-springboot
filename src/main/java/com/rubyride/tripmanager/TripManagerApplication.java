package com.rubyride.tripmanager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TripManagerApplication {
  private static final Logger log = LoggerFactory.getLogger(TripManagerApplication.class);

  public static void main(final String[] args) {
    log.debug("REDIS_HOST=" + System.getenv().get("REDIS_HOST"));
    log.debug("REDIS_PORT=" + System.getenv().get("REDIS_PORT"));

    log.debug("MONGODB_HOST=" + System.getenv().get("MONGODB_HOST"));
    log.debug("MONGODB_PORT=" + System.getenv().get("MONGODB_PORT"));
    log.debug("MONGODB_NAME=" + System.getenv().get("MONGODB_NAME"));

    SpringApplication.run(TripManagerApplication.class, args);
  }
}
