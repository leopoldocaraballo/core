package com.vengalsas.core.auth.infrastructure.security;

import java.util.UUID;

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
        .map(UserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
  }

  public UserDetails loadUserById(UUID id) {
    return userRepository.findById(id)
        .map(UserPrincipal::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
  }
}
