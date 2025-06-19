package com.vengalsas.core.auth.infrastructure.security;

import java.security.KeyPair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.vengalsas.core.auth.domain.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${security.jwt.access-token-expiration-minutes:15}")
  private long accessTokenExpirationMinutes;

  private final KeyPair keyPair; // Inyectado como @Bean en Config

  public String generateAccessToken(User user, String jti, String sessionId) {
    Instant now = Instant.now();
    Instant expiry = now.plus(accessTokenExpirationMinutes, ChronoUnit.MINUTES);

    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("role", user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toList()))
        .claim("sid", sessionId)
        .setId(jti)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiry))
        .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
        .compact();
  }

  public String generateRefreshToken() {
    return UUID.randomUUID().toString();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(keyPair.getPublic())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}
