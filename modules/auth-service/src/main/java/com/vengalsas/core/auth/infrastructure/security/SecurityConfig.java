package com.vengalsas.core.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Rutas públicas
            .requestMatchers(
                "/api/v1/auth/login",
                "/api/v1/auth/refresh-token",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/actuator/**")
            .permitAll()

            // Solo ADMIN o SUPERADMIN pueden registrar
            .requestMatchers(HttpMethod.POST, "/api/v1/auth/register")
            .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPERADMIN")

            // Perfil disponible para cualquier rol autenticado
            .requestMatchers(HttpMethod.GET, "/api/v1/auth/me")
            .hasAnyAuthority("ROLE_USER", "ROLE_ADMIN", "ROLE_SUPERADMIN")

            .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()

            .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/disable").hasAuthority("ROLE_SUPERADMIN")
            .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*/enable").hasAuthority("ROLE_SUPERADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/users/*/soft-delete").hasAuthority("ROLE_SUPERADMIN")

            // Todas las demás requieren autenticación
            .anyRequest().authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
