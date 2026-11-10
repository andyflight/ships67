package com.games.shipmaster.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    <S extends RefreshToken> S save(S token);

    Optional<RefreshToken> findById(UUID id);

    /** Locks the row so concurrent refreshes of one token are serialized. */
    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);

    /** Revokes every not-yet-revoked token of the family; returns how many. */
    int revokeFamily(UUID familyId, Instant now);
}
