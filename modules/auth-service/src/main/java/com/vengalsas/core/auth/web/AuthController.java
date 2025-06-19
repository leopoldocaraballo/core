package com.vengalsas.core.auth.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vengalsas.core.auth.application.service.AuthService;
import com.vengalsas.core.auth.domain.model.User;
import com.vengalsas.core.auth.web.dto.request.LoginRequest;
import com.vengalsas.core.auth.web.dto.request.RefreshTokenRequest;
import com.vengalsas.core.auth.web.dto.request.RegisterRequest;
import com.vengalsas.core.auth.web.dto.response.LoginResponse;
import com.vengalsas.core.auth.web.dto.response.RegisterResponse;
import com.vengalsas.core.auth.web.dto.response.UserProfileResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    RegisterResponse response = authService.register(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = authService.authenticate(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal User user) {
    UserProfileResponse response = authService.getProfile(user);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    LoginResponse response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }
}