package com.games.shipmaster.auth.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.games.shipmaster.auth.domain.SecretCodec;

/**
 * 256-bit random secrets, stored as unsalted SHA-256: fine for random tokens (unlike passwords) and lets the
 * database look them up by hash.
 */
@Component
public class Sha256SecretCodec implements SecretCodec {

    static final int SECRET_BYTES = 32;
    /** Versioned prefix: recognizable in logs and leak scanners, and lets the format change later. */
    static final String GUEST_CREDENTIAL_PREFIX = "g1.";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String newRefreshToken() {
        return randomBase64Url();
    }

    @Override
    public String newGuestCredential() {
        return GUEST_CREDENTIAL_PREFIX + randomBase64Url();
    }

    @Override
    public String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the JDK spec", e);
        }
    }

    private String randomBase64Url() {
        byte[] bytes = new byte[SECRET_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
