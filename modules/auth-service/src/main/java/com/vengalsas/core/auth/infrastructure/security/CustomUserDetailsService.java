package com.vengalsas.core.auth.infrastructure.security;

import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.vengalsas.core.auth.domain.model.User;
import com.vengalsas.core.auth.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository.findByEmailIgnoreCase(email)
        .map(this::toPrincipal)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  public UserDetails loadUserById(UUID id) {
    return userRepository.findById(id)
        .map(this::toPrincipal)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  private UserDetails toPrincipal(User user) {
    return org.springframework.security.core.userdetails.User
        .withUsername(user.getEmail())
        .password(user.getPassword())
        .authorities(user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
            .toList())
        .accountLocked(user.isAccountLocked())
        .disabled(!user.isActive())
        .build();
  }
}
