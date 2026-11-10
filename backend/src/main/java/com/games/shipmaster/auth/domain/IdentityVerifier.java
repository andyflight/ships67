package com.games.shipmaster.auth.domain;

import java.util.Map;

/**
 * Verifies a platform sign-in proof (Game Center signature, Play Games auth code, Apple identity token) and returns
 * the stable provider subject. No implementations yet: account linking is a later change (design D8).
 */
public interface IdentityVerifier {

    AuthProvider provider();

    VerifiedSubject verify(ProviderCredential credential);

    record ProviderCredential(AuthProvider provider, Map<String, String> fields) {
    }

    record VerifiedSubject(AuthProvider provider, String subject) {
    }
}
