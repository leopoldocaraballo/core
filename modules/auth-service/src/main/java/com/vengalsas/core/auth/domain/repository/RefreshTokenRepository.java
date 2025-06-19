package com.vengalsas.core.auth.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vengalsas.core.auth.domain.model.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  // puedes agregar métodos si necesitas búsquedas más específicas
}
