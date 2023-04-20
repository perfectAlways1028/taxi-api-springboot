package com.rubyride.tripmanager.utility;

import com.rubyride.tripmanager.repository.mongo.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserUtils {
  private final UserRepository userRepository;

  public UserUtils(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public boolean userAlreadyExists(final String username) {
    return userRepository.findByUserName(username) != null;
  }

  public String generateUsername(final String firstName, final String lastName) {
    final var baseName = (ObjectUtils.getOrDefault(firstName, "a") + ObjectUtils.getOrDefault(lastName, ""))
        .replaceAll("[\\W]", "")
        .toLowerCase();
    var generatedName = baseName;
    var appendValue = 0;

    while (userAlreadyExists(generatedName)) {
      generatedName = baseName + (++appendValue);
    }

    return generatedName;
  }
}
