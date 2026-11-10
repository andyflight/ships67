package com.games.shipmaster.auth.infrastructure;

import java.time.Duration;
import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Auth settings from {@code shipmaster.auth.*}. Construction fails (and so does startup) on a missing or weak secret.
 */
@ConfigurationProperties("shipmaster.auth")
public record AuthProperties(
        String jwtSecret,
        String issuer,
        Duration accessTtl,
        Duration refreshTtl,
        Duration refreshGrace) {

    static final int MIN_SECRET_BYTES = 32;

    public AuthProperties {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "shipmaster.auth.jwt-secret is not set: export SHIPMASTER_JWT_SECRET (openssl rand -base64 32)");
        }
        if (secretBytes(jwtSecret).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "shipmaster.auth.jwt-secret must be base64 of at least " + MIN_SECRET_BYTES + " bytes");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("shipmaster.auth.issuer must be set");
        }
        requirePositive("access-ttl", accessTtl);
        requirePositive("refresh-ttl", refreshTtl);
        requirePositive("refresh-grace", refreshGrace);
    }

    public byte[] secretBytes() {
        return secretBytes(jwtSecret);
    }

    private static byte[] secretBytes(String secret) {
        try {
            return Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("shipmaster.auth.jwt-secret is not valid base64", e);
        }
    }

    private static void requirePositive(String name, Duration value) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException("shipmaster.auth." + name + " must be set");
        }
    }
}
