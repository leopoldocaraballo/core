package com.vengalsas.core.auth.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.vengalsas.core.auth.domain.model.BusinessException;
import com.vengalsas.core.auth.domain.model.RefreshToken;
import com.vengalsas.core.auth.domain.model.Role;
import com.vengalsas.core.auth.domain.model.RoleType;
import com.vengalsas.core.auth.domain.model.User;
import com.vengalsas.core.auth.domain.repository.RefreshTokenRepository;
import com.vengalsas.core.auth.domain.repository.RoleRepository;
import com.vengalsas.core.auth.domain.repository.UserRepository;
import com.vengalsas.core.auth.infrastructure.security.JwtTokenProvider;
import com.vengalsas.core.auth.web.dto.request.LoginRequest;
import com.vengalsas.core.auth.web.dto.request.RegisterRequest;
import com.vengalsas.core.auth.web.dto.response.LoginResponse;
import com.vengalsas.core.auth.web.dto.response.RegisterResponse;
import com.vengalsas.core.auth.web.dto.response.UserProfileResponse;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  private static final int MAX_LOGIN_ATTEMPTS = 5;
  private static final long ACCESS_TOKEN_TTL_MINUTES = 15;
  private static final long REFRESH_TOKEN_TTL_DAYS = 7;

  @Transactional
  public RegisterResponse register(RegisterRequest request) {
    String email = request.getEmail().toLowerCase().trim();

    if (email.isEmpty() || request.getPassword().isEmpty()) {
      throw new IllegalArgumentException("Email y contraseña no pueden estar vacíos");
    }

    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new IllegalArgumentException("Las contraseñas no coinciden");
    }

    if (!request.isAcceptedPrivacyPolicy()) {
      throw new IllegalArgumentException("Debes aceptar la política de privacidad");
    }

    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new IllegalArgumentException("El correo ya está en uso");
    }

    Role userRole = roleRepository.findByName(RoleType.USER)
        .orElseThrow(() -> new IllegalStateException("Rol por defecto USER no encontrado"));

    User user = User.builder()
        .email(email)
        .password(passwordEncoder.encode(request.getPassword()))
        .roles(Set.of(userRole))
        .consentAcceptedAt(Instant.now())
        .privacyPolicyVersion(request.getPrivacyPolicyVersion())
        .build();

    userRepository.save(user);

    return new RegisterResponse("Registro exitoso. Por favor verifica tu correo electrónico.");
  }

  @Transactional
  public LoginResponse authenticate(LoginRequest request) {
    String email = request.getEmail().trim();

    User user = userRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new IllegalArgumentException("Credenciales inválidas"));

    if (!user.isActive())
      throw new IllegalStateException("Cuenta desactivada");
    if (user.isAccountLocked())
      throw new IllegalStateException("Cuenta bloqueada");
    if (!user.isEmailVerified())
      throw new IllegalStateException("Correo no verificado");

    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      user.setLoginAttempts(user.getLoginAttempts() + 1);
      if (user.getLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
        user.setAccountLocked(true);
      }
      userRepository.save(user);
      throw new IllegalArgumentException("Credenciales inválidas");
    }

    user.setLoginAttempts(0);
    userRepository.save(user);

    String sessionId = UUID.randomUUID().toString();
    String jti = UUID.randomUUID().toString();
    String accessToken = jwtTokenProvider.generateAccessToken(user, jti, sessionId);
    String refreshTokenRaw = jwtTokenProvider.generateRefreshToken();

    RefreshToken refreshToken = RefreshToken.builder()
        .user(user)
        .tokenHash(passwordEncoder.encode(refreshTokenRaw))
        .sessionId(sessionId)
        .deviceFingerprint(request.getDeviceFingerprint())
        .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(refreshToken);

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenRaw)
        .expiresIn(ACCESS_TOKEN_TTL_MINUTES * 60)
        .build();
  }

  public UserProfileResponse getProfile(User user) {
    return UserProfileResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toUnmodifiableSet()))
        .emailVerified(user.isEmailVerified())
        .accountLocked(user.isAccountLocked())
        .active(user.isActive())
        .createdAt(user.getCreatedAt())
        .lastUpdated(user.getUpdatedAt())
        .build();
  }

  @Transactional
  public LoginResponse refreshToken(String refreshTokenRaw) {
    RefreshToken oldToken = refreshTokenRepository.findAll().stream()
        .filter(rt -> passwordEncoder.matches(refreshTokenRaw, rt.getTokenHash()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

    if (oldToken.isRevoked() || oldToken.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalStateException("Refresh token revocado o expirado");
    }

    oldToken.setRevoked(true);
    refreshTokenRepository.save(oldToken);

    User user = oldToken.getUser();
    String newJti = UUID.randomUUID().toString();
    String newAccessToken = jwtTokenProvider.generateAccessToken(user, newJti, oldToken.getSessionId());
    String newRefreshTokenRaw = jwtTokenProvider.generateRefreshToken();

    RefreshToken newRefreshToken = RefreshToken.builder()
        .user(user)
        .tokenHash(passwordEncoder.encode(newRefreshTokenRaw))
        .sessionId(oldToken.getSessionId())
        .deviceFingerprint(oldToken.getDeviceFingerprint())
        .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(newRefreshToken);

    return LoginResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshTokenRaw)
        .expiresIn(ACCESS_TOKEN_TTL_MINUTES * 60)
        .build();
  }

  @Transactional
  public void logout(String refreshToken, String deviceFingerprint) {
    var token = refreshTokenRepository.findAll().stream()
        .filter(rt -> passwordEncoder.matches(refreshToken, rt.getTokenHash()))
        .findFirst()
        .orElseThrow(() -> new BusinessException("Refresh token no válido"));

    if (!token.getDeviceFingerprint().equals(deviceFingerprint)) {
      throw new BusinessException("Dispositivo no coincide con la sesión");
    }

    token.setRevoked(true);
    refreshTokenRepository.save(token);
  }
}
