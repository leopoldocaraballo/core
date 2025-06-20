package com.vengalsas.core.auth.application.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.vengalsas.core.auth.domain.model.User;
import com.vengalsas.core.auth.web.dto.response.UserProfileResponse;

@Component
public class UserMapper {

  public UserProfileResponse toProfileResponse(User user) {
    return UserProfileResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet()))
        .emailVerified(user.isEmailVerified())
        .accountLocked(user.isAccountLocked())
        .active(user.isActive())
        .createdAt(user.getCreatedAt())
        .lastUpdated(user.getUpdatedAt())
        .build();
  }
}
