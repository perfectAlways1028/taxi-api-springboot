package com.rubyride.tripmanager.service;

import com.rubyride.model.Role;
import com.rubyride.model.User;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserDetailsService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;

  public UserServiceImpl(final UserRepository userRepository, final BCryptPasswordEncoder bCryptPasswordEncoder) {
    this.userRepository = userRepository;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
  }

  @PostConstruct
  public void bootstrapAdminUser() {
    Optional.ofNullable(userRepository.findByUserName("admin"))
        .ifPresentOrElse(admin -> {},
            () -> {
              userRepository.insert(new User()
                  .id(UUID.randomUUID())
                  .userName("admin")
                  .password(bCryptPasswordEncoder.encode("Rubyr!d3"))
                  .active(true)
                  .created(OffsetDateTime.now())
                  .roles(List.of(
                      Role.ADMIN,
                      Role.DISPATCHER,
                      Role.DRIVER,
                      Role.RIDER)));
        });
  }

  @Override
  public UserDetails loadUserByUsername(final String usernameOrEmail) throws UsernameNotFoundException {
    var user = userRepository.findByUserName(usernameOrEmail);
    if (user == null) {
      // try again with e-mail
      user = userRepository.findByEmail(usernameOrEmail);

      if (user == null) {
        throw new UsernameNotFoundException(usernameOrEmail);
      }
    }

    return new org.springframework.security.core.userdetails.User(
        user.getUserName(),
        user.getPassword(),
        StreamUtils.safeStream(user.getRoles())
            .map(Role::toString)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList()));
  }
}
