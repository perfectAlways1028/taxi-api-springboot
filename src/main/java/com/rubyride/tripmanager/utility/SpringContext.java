package com.rubyride.tripmanager.utility;

import com.rubyride.model.Role;
import com.rubyride.model.User;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import com.rubyride.tripmanager.security.SecurityConstants;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SpringContext implements ApplicationContextAware {
  private final UserRepository userRepository;
  private ApplicationContext context;

  public SpringContext(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Returns the Spring managed bean instance of the given class type if it exists.
   * Returns null otherwise.
   *
   * @param beanClass class for which to lookup bean
   * @return instance of bean of matching class
   */
  public <T> T getBean(final Class<T> beanClass) {
    return context.getBean(beanClass);
  }

  public Optional<String> getAuthenticatedUserName() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getPrincipal)
        .map(Object::toString);
  }

  public String getAuthenticatedUserNameFromRequest(final HttpServletRequest request) {
    return TokenUtils.getUserNameFromToken(request.getHeader(SecurityConstants.HEADER_STRING));
  }

  public Optional<User> getAuthenticatedUser() {
    return getAuthenticatedUserName()
        .map(userRepository::findByUserName);
  }

  public Optional<UUID> getAuthenticatedUserId() {
    return getAuthenticatedUser()
        .map(User::getId);
  }

  public Optional<UUID> getAuthenticatedUserIdFromRequest(final HttpServletRequest request) {
    return Optional.ofNullable(userRepository.findByUserName(getAuthenticatedUserNameFromRequest(request)))
        .map(User::getId);
  }

  public List<Role> getAuthenticatedUserRoles() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
        .map(Authentication::getAuthorities)
        .orElse(Collections.emptyList())
        .stream()
        .map(GrantedAuthority::getAuthority)
        .map(Role::fromValue)
        .collect(Collectors.toList());
  }

  @Override
  public void setApplicationContext(final ApplicationContext context) throws BeansException {
    this.context = context;
  }
}
