package com.rubyride.tripmanager.api;

import com.rubyride.api.ContactApi;
import com.rubyride.tripmanager.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import java.util.Map;

@Configuration
@EnableConfigurationProperties
@RestController
public class ContactApiImpl implements ContactApi {
  private static final Logger log = LoggerFactory.getLogger(ContactApiImpl.class);

  private final EmailService emailService;

  @Value("${email.contact:}")
  private String toEmailAddress;

  @Autowired
  public ContactApiImpl(final EmailService emailService) {
    this.emailService = emailService;
  }

  @Override
  public ResponseEntity<Void> contactUs(@NotNull @Valid final String name, @NotNull @Email @Valid final String email, @Valid final String body) {
    try {
      emailService.sendTemplatedEmail(
          toEmailAddress,
          "Website Contact",
          "contact-us",
          Map.of("fromName", name,
              "fromEmail", email,
              "text", body),
          null);
    } catch (final MessagingException e) {
      log.error("MessagingException caught", e);
      throw new RuntimeException(e);
    }

    return ResponseEntity.ok()
        .build();
  }
}
