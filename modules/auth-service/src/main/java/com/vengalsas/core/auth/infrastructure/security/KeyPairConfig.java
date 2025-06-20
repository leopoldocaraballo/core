package com.vengalsas.core.auth.infrastructure.security;

import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeyPairConfig {

  @Value("${jwt.keystore.path}")
  private String keystorePath;

  @Value("${jwt.keystore.password}")
  private String keystorePassword;

  @Value("${jwt.key.alias}")
  private String keyAlias;

  @Value("${jwt.key.password}")
  private String keyPassword;

  @Bean
  public KeyPair keyPair() {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(keystorePath)) {
      if (is == null) {
        throw new IllegalStateException("Keystore not found in classpath: " + keystorePath);
      }

      KeyStore keyStore = KeyStore.getInstance("PKCS12");
      keyStore.load(is, keystorePassword.toCharArray());

      Key key = keyStore.getKey(keyAlias, keyPassword.toCharArray());
      Certificate cert = keyStore.getCertificate(keyAlias);

      if (cert == null) {
        throw new IllegalStateException("Certificate not found for alias: " + keyAlias);
      }

      return new KeyPair(cert.getPublicKey(), (PrivateKey) key);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot load keypair from keystore", e);
    }
  }
}
