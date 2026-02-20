package com.example.backend.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
public class SecurityConfig {

  private final ApiKeyAuthFilter apiKeyAuthFilter;

  public SecurityConfig(ApiKeyAuthFilter apiKeyAuthFilter) {
    this.apiKeyAuthFilter = apiKeyAuthFilter;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    http.authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/**").permitAll()
        .anyRequest().permitAll());

    http.addFilterBefore(
        apiKeyAuthFilter,
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

    http.httpBasic(hb -> hb.disable());
    http.formLogin(fl -> fl.disable());

    return http.build();
  }
}
