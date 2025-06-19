package com.vengalsas.core.auth.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

  @Email
  @NotBlank
  private String email;

  @NotBlank
  @Size(min = 8, max = 120)
  private String password;

  @NotBlank
  private String confirmPassword;

  private boolean acceptedPrivacyPolicy;

  private String privacyPolicyVersion;

  private String captchaToken;
}