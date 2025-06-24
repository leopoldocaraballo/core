package com.vengalsas.core.auth.web.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DeleteUserRequest {
  @NotNull
  private UUID userId;
}
