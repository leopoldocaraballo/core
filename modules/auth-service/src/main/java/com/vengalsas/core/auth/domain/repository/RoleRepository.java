package com.vengalsas.core.auth.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vengalsas.core.auth.domain.model.Role;
import com.vengalsas.core.auth.domain.model.RoleType;

public interface RoleRepository extends JpaRepository<Role, UUID> {
  Optional<Role> findByName(RoleType name);
}
