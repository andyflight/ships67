package com.games.shipmaster.support;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.games.shipmaster.auth.domain.RefreshToken;
import com.games.shipmaster.auth.domain.RefreshTokenRepository;

public final class InMemoryRefreshTokens implements RefreshTokenRepository {

    public final Map<UUID, RefreshToken> byId = new LinkedHashMap<>();

    @Override
    public <S extends RefreshToken> S save(S token) {
        byId.put(token.getId(), token);
        return token;
    }

    @Override
    public Optional<RefreshToken> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return byId.values().stream().filter(t -> t.getTokenHash().equals(tokenHash)).findFirst();
    }

    @Override
    public int revokeFamily(UUID familyId, Instant now) {
        int count = 0;
        for (RefreshToken t : byId.values()) {
            if (t.getFamilyId().equals(familyId) && !t.isRevoked()) {
                t.revoke(now);
                count++;
            }
        }
        return count;
    }
}
