package com.rubyride.tripmanager.security;

import com.rubyride.tripmanager.service.UserServiceImpl;
import com.rubyride.tripmanager.utility.SpringContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
  private final UserServiceImpl userDetailsService;
  private final BCryptPasswordEncoder bCryptPasswordEncoder;
  private final JWTLogoutHandler logoutHandler;
  private final SpringContext springContext;

  public SecurityConfig(final UserServiceImpl userDetailsService, final BCryptPasswordEncoder bCryptPasswordEncoder, final JWTLogoutHandler logoutHandler, final SpringContext springContext) {
    this.userDetailsService = userDetailsService;
    this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    this.logoutHandler = logoutHandler;
    this.springContext = springContext;
  }

  @Override
  public void configure(final WebSecurity web) {
    final var firewall = new DefaultHttpFirewall();
    firewall.setAllowUrlEncodedSlash(true);
    web.httpFirewall(firewall);
  }

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    http.cors()
        .and()
        .csrf().disable()
        .authorizeRequests()
        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll() // **permit OPTIONS call to all**
        .antMatchers(HttpMethod.POST,
            SecurityConstants.USER_CREATION_URL,
            SecurityConstants.PASSWORD_RESET_URL,
            SecurityConstants.CONTACT_URL).permitAll()
        .antMatchers(HttpMethod.GET,
            SecurityConstants.ACTUATOR_URL,
            SecurityConstants.WEBSOCKET_URL,
            SecurityConstants.USER_EXISTS_URL,
            SecurityConstants.LOGOUT_URL).permitAll()
        .anyRequest().authenticated()
        .and()
        .addFilter(new JWTAuthenticationFilter(authenticationManager(), springContext))
        .addFilter(new JWTAuthorizationFilter(authenticationManager(), springContext))
        .logout(logout -> logout.addLogoutHandler(logoutHandler)
            .logoutSuccessHandler((request, response, authorization) -> response.setStatus(HttpServletResponse.SC_OK)))
        // this disables session creation on Spring Security
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

  @Override
  public void configure(final AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService)
        .passwordEncoder(bCryptPasswordEncoder);
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final var configuration = new CorsConfiguration().applyPermitDefaultValues();
    configuration.setAllowedMethods(List.of(
        HttpMethod.GET.name(),
        HttpMethod.HEAD.name(),
        HttpMethod.POST.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.PUT.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.OPTIONS.name()
    ));
    configuration.setAllowedOrigins(List.of("*"));
    configuration.addAllowedHeader("*");

    configuration.addExposedHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
    final var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
