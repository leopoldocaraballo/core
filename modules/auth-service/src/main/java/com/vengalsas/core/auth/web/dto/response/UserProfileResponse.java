package com.vengalsas.core.auth.web.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
  private UUID id;
  private String email;
  private Set<String> roles;
  private boolean emailVerified;
  private boolean accountLocked;
  private boolean active;
  private Instant createdAt;
  private Instant lastUpdated;
}