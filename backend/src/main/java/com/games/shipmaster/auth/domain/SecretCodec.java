package com.games.shipmaster.auth.domain;

/** Generates high-entropy secrets and the hashes stored in their place. */
public interface SecretCodec {

    String newRefreshToken();

    String newGuestCredential();

    String hash(String secret);
}
