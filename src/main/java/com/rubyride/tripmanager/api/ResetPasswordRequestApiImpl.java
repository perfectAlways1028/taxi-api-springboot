package com.rubyride.tripmanager.api;

import com.google.common.base.Strings;
import com.rubyride.model.PasswordReset;
import com.rubyride.model.User;
import com.rubyride.model.UserPassword;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.PlaceRepository;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.security.TokenManager;
import com.rubyride.tripmanager.service.EmailService;
import com.rubyride.tripmanager.utility.SpringContext;
import com.rubyride.tripmanager.utility.UserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties
@Service
public class ResetPasswordRequestApiImpl {
  private static final Logger log = LoggerFactory.getLogger(ResetPasswordRequestApiImpl.class);

  private final UserRepository userRepository;
  private final EmailService emailService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  @Value("${email.from:}")
  private String fromEmailAddress;
  @Value("${environment.baseUrl:}")
  private String environmentBaseUrl;
  @Value("${environment.resetLimitInMinutes:}")
  private int resetLimitInMinutes;

  @Autowired
  public ResetPasswordRequestApiImpl(final UserRepository userRepository, final PlaceRepository placeRepository, final TokenManager tokenManager, final EmailService eMailService, final BCryptPasswordEncoder bCryptPasswordEncoder, final SpringContext springContext, final UserUtils userUtils) {
    this.userRepository = userRepository;
    this.emailService = eMailService;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
  }

  public ResponseEntity<Void> requestPasswordReset(final String userName) {
    Optional.ofNullable(userRepository.findByUserName(userName))
        .filter(user -> !Strings.isNullOrEmpty(user.getEmail()))
        .ifPresentOrElse(
            user -> {
              userRepository.save(
                  user.resetPassword(new PasswordReset()
                      .requestId(UUID.randomUUID())
                      .expires(OffsetDateTime.now().plusMinutes(resetLimitInMinutes))));

              sendPasswordResetEmail(user);
            },
            () -> {
              throw new EntityNotFoundException("Username not found");
            });

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<Void> changePassword(final String userName, final String resetId, final UserPassword password) {
    byte[] decodedBytes = Base64.getDecoder().decode(userName);
    String decodedUsername  = new String(decodedBytes);

    Optional.ofNullable(userRepository.findByUserName(decodedUsername))
        .filter(user -> user.getResetPassword() != null)
        .filter(user -> user.getResetPassword().getRequestId().equals(UUID.fromString(resetId)))
        .filter(user -> user.getResetPassword().getExpires().isAfter(OffsetDateTime.now()))
        .ifPresentOrElse(
            user -> userRepository.save(
                user.password(bCryptPasswordEncoder.encode(password.getPassword()))
                    .resetPassword(null)),
            () -> {
              throw new EntityNotFoundException("Username or valid password reset request not found");
            });

    return ResponseEntity.noContent()
        .build();
  }

  private void sendPasswordResetEmail(final User user) {
    try {
      String encodedUsername = Base64.getEncoder().encodeToString(user.getUserName().getBytes());
      emailService.sendTemplatedEmail(
          user.getEmail(),
          "Your Reset Request",
          "password-reset",
          Map.of(
              "fromName", "Ruby Ride",
              "fromEmail", fromEmailAddress,
              "baseUrl", System.getenv().getOrDefault("BASE_URL", environmentBaseUrl),
              "recipientName", user.getFirstName(),
              "userName", encodedUsername,
              "resetKey", user.getResetPassword().getRequestId()
          ),
          null);
    } catch (final MessagingException e) {
      log.error("MessagingException caught", e);
      throw new RuntimeException(e);
    }
  }
}
