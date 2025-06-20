package com.vengalsas.core.auth.config;

import java.time.Instant;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.vengalsas.core.auth.domain.model.Role;
import com.vengalsas.core.auth.domain.model.RoleType;
import com.vengalsas.core.auth.domain.model.User;
import com.vengalsas.core.auth.domain.repository.RoleRepository;
import com.vengalsas.core.auth.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;

  @Bean
  public CommandLineRunner initSuperAdmin() {
    return args -> {
      // Crear roles si no existen
      for (RoleType type : RoleType.values()) {
        roleRepository.findByName(type)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(type)
                .description("Rol " + type.name())
                .build()));
      }

      // Crear usuario SUPERADMIN si no existe
      final String email = "leopoldo.caraballo@vengalsas.com";
      if (!userRepository.existsByEmailIgnoreCase(email)) {
        Role superAdminRole = roleRepository.findByName(RoleType.SUPERADMIN)
            .orElseGet(() -> roleRepository.save(Role.builder()
                .name(RoleType.SUPERADMIN)
                .description("Rol SUPERADMIN")
                .build()));

        User superAdmin = User.builder()
            .email(email)
            .password(passwordEncoder.encode("Vengal#2025"))
            .roles(Set.of(superAdminRole))
            .emailVerified(true)
            .accountLocked(false)
            .active(true)
            .consentAcceptedAt(Instant.now())
            .privacyPolicyVersion("1.0")
            .build();

        userRepository.save(superAdmin);
        System.out.println("✔ Usuario SUPERADMIN creado por defecto.");
      } else {
        System.out.println("ℹ Usuario SUPERADMIN ya existe.");
      }
    };
  }
}
