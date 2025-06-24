package com.vengalsas.core.auth.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vengalsas.core.auth.application.mapper.UserMapper;
import com.vengalsas.core.auth.application.service.AuthService;
import com.vengalsas.core.auth.infrastructure.security.UserPrincipal;
import com.vengalsas.core.auth.web.dto.request.LoginRequest;
import com.vengalsas.core.auth.web.dto.request.LogoutRequest;
import com.vengalsas.core.auth.web.dto.request.RefreshTokenRequest;
import com.vengalsas.core.auth.web.dto.request.RegisterRequest;
import com.vengalsas.core.auth.web.dto.response.LoginResponse;
import com.vengalsas.core.auth.web.dto.response.RegisterResponse;
import com.vengalsas.core.auth.web.dto.response.UserProfileResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication API", description = "Operaciones de autenticación y autorización")
public class AuthController {

  private final AuthService authService;
  private final UserMapper userMapper;

  // 👤 Perfil
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(userMapper.toProfileResponse(principal.getUser()));
  }

  // 🧾 Registro y login
  @PostMapping("/register")
  public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.authenticate(request));
  }

  // 🔁 Tokens
  @PostMapping("/refresh-token")
  public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
    authService.logout(request.getRefreshToken(), request.getDeviceFingerprint());
    return ResponseEntity.noContent().build();
  }

  // 🔒 Gestión de usuarios (solo ADMIN/SUPERADMIN)
  @PatchMapping("/{id}/disable")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
  public ResponseEntity<Void> disableUser(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
    authService.disableUser(id, principal.getUser());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/enable")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
  public ResponseEntity<Void> enableUser(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
    authService.enableUser(id, principal.getUser());
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/soft-delete")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERADMIN')")
  public ResponseEntity<Void> softDelete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
    authService.softDeleteUser(id, principal.getUser());
    return ResponseEntity.noContent().build();
  }
}