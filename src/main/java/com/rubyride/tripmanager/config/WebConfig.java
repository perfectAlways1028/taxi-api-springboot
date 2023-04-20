package com.rubyride.tripmanager.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Configuration
public class WebConfig extends WebMvcConfigurationSupport {
  @Override
  public void addFormatters(final FormatterRegistry registry) {
    final var registrar = new DateTimeFormatterRegistrar();
    registrar.setDateFormatter(DateTimeFormatter.ISO_LOCAL_DATE);
    registrar.setDateTimeFormatter(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    registrar.registerFormatters(registry);
  }

  @Override
  public void extendMessageConverters(final List<HttpMessageConverter<?>> converters) {
    converters.stream()
        .filter(converter -> MappingJackson2HttpMessageConverter.class.isAssignableFrom(converter.getClass()))
        .map(MappingJackson2HttpMessageConverter.class::cast)
        .map(MappingJackson2HttpMessageConverter::getObjectMapper)
        .forEach(objectMapper -> {
          objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
          objectMapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        });
  }
}