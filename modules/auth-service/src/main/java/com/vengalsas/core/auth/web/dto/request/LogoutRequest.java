package com.vengalsas.core.auth.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

  @NotBlank(message = "El refresh token es obligatorio")
  private String refreshToken;

  @NotBlank(message = "El device fingerprint es obligatorio")
  private String deviceFingerprint;
}
