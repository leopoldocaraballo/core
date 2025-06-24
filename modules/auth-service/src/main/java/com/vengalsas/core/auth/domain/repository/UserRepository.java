package com.vengalsas.core.auth.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vengalsas.core.auth.domain.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByEmailIgnoreCase(String email);

  boolean existsByEmailIgnoreCase(String email);

  @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
  Optional<User> findByEmailWithRoles(@Param("email") String email);
}
