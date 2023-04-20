package com.rubyride.tripmanager.repository.redis;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Component
@ReadingConverter
public class BytesToOffsetDateTimeConverter implements Converter<byte[], OffsetDateTime> {
  @Override
  public OffsetDateTime convert(final byte[] source) {
    return OffsetDateTime.parse(new String(source, StandardCharsets.UTF_8), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
  }
}