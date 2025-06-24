package com.vengalsas.core.auth.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    if (email.isEmpty() || request.getPassword().isEmpty())
      throw new IllegalArgumentException("Email y contraseña no pueden estar vacíos");

    if (!request.getPassword().equals(request.getConfirmPassword()))
      throw new IllegalArgumentException("Las contraseñas no coinciden");

    if (!request.isAcceptedPrivacyPolicy())
      throw new IllegalArgumentException("Debes aceptar la política de privacidad");

    if (userRepository.existsByEmailIgnoreCase(email))
      throw new IllegalArgumentException("El correo ya está en uso");

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

    User user = userRepository.findByEmailWithRoles(email)
        .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

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

    user.setLoginAttempts(0); // reset
    userRepository.save(user);

    return generateLoginResponse(user, request.getDeviceFingerprint());
  }

  public UserProfileResponse getProfile(User user) {
    return UserProfileResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .roles(user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toUnmodifiableSet()))
        // .emailVerified(user.isEmailVerified())
        .emailVerified(true)
        .accountLocked(user.isAccountLocked())
        .active(user.isActive())
        .createdAt(user.getCreatedAt())
        .lastUpdated(user.getUpdatedAt())
        .build();
  }

  @Transactional
  public LoginResponse refreshToken(String refreshTokenRaw) {
    RefreshToken oldToken = findValidRefreshToken(refreshTokenRaw);

    oldToken.setRevoked(true);
    refreshTokenRepository.save(oldToken);

    return generateLoginResponse(oldToken.getUser(), oldToken.getDeviceFingerprint(), oldToken.getSessionId());
  }

  @Transactional
  public void logout(String refreshTokenRaw, String deviceFingerprint) {
    RefreshToken token = findValidRefreshToken(refreshTokenRaw);

    if (!token.getDeviceFingerprint().equals(deviceFingerprint))
      throw new BusinessException("Dispositivo no coincide con la sesión");

    token.setRevoked(true);
    refreshTokenRepository.save(token);
  }

  @Transactional
  public void softDeleteUser(UUID userId, User actor) {
    User user = getUserById(userId);
    checkAccessControl(actor, user);
    user.setActive(false);
    user.setAccountLocked(true);
    userRepository.save(user);
  }

  @Transactional
  public void disableUser(UUID userId, User actor) {
    User user = getUserById(userId);
    checkAccessControl(actor, user);
    user.setActive(false);
    userRepository.save(user);
  }

  @Transactional
  public void enableUser(UUID userId, User actor) {
    User user = getUserById(userId);
    checkAccessControl(actor, user);
    user.setActive(true);
    user.setAccountLocked(false);
    userRepository.save(user);
  }

  // ---------- MÉTODOS PRIVADOS AUXILIARES ----------

  private RefreshToken findValidRefreshToken(String rawToken) {
    return refreshTokenRepository.findAll().stream()
        .filter(rt -> passwordEncoder.matches(rawToken, rt.getTokenHash()))
        .filter(rt -> !rt.isRevoked() && rt.getExpiresAt().isAfter(Instant.now()))
        .findFirst()
        .orElseThrow(() -> new BusinessException("Refresh token inválido o expirado"));
  }

  private LoginResponse generateLoginResponse(User user, String fingerprint) {
    String sessionId = UUID.randomUUID().toString();
    return generateLoginResponse(user, fingerprint, sessionId);
  }

  private LoginResponse generateLoginResponse(User user, String fingerprint, String sessionId) {
    String jti = UUID.randomUUID().toString();
    String accessToken = jwtTokenProvider.generateAccessToken(user, jti, sessionId);
    String refreshTokenRaw = jwtTokenProvider.generateRefreshToken();

    RefreshToken refreshToken = RefreshToken.builder()
        .user(user)
        .tokenHash(passwordEncoder.encode(refreshTokenRaw))
        .sessionId(sessionId)
        .deviceFingerprint(fingerprint)
        .expiresAt(Instant.now().plus(REFRESH_TOKEN_TTL_DAYS, ChronoUnit.DAYS))
        .build();

    refreshTokenRepository.save(refreshToken);

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshTokenRaw)
        .expiresIn(ACCESS_TOKEN_TTL_MINUTES * 60)
        .build();
  }

  private User getUserById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
  }

  private void checkAccessControl(User actor, User target) {
    if (actor.getId().equals(target.getId()))
      throw new BusinessException("No puedes modificarte a ti mismo");

    boolean isActorSuperadmin = actor.hasRole(RoleType.SUPERADMIN);
    boolean isTargetSuperadmin = target.hasRole(RoleType.SUPERADMIN);
    boolean isTargetAdmin = target.hasRole(RoleType.ADMIN);

    if (!isActorSuperadmin && (isTargetSuperadmin || isTargetAdmin))
      throw new BusinessException("No puedes modificar este usuario");

    if (!actor.hasRole(RoleType.SUPERADMIN) && !actor.hasRole(RoleType.ADMIN))
      throw new BusinessException("No tienes permisos para esta operación");
  }
}
