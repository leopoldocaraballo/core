package com.vengalsas.core.auth.infrastructure.security;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
  }

  public UserDetails loadUserById(UUID id) {
    return userRepository.findById(id)
        .map(this::toPrincipal)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
  }

  private UserDetails toPrincipal(com.vengalsas.core.auth.domain.model.User user) {
    return User.withUsername(user.getEmail())
        .password(user.getPassword())
        .authorities(user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
            .collect(Collectors.toList()))
        .accountLocked(user.isAccountLocked())
        .disabled(!user.isActive())
        .build();
  }
}
