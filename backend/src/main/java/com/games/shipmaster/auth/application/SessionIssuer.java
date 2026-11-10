package com.games.shipmaster.auth.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.games.shipmaster.auth.domain.AccessTokenIssuer;
import com.games.shipmaster.auth.domain.IssuedAccessToken;
import com.games.shipmaster.auth.domain.RefreshToken;
import com.games.shipmaster.auth.domain.RefreshTokenRepository;
import com.games.shipmaster.auth.domain.SecretCodec;

import lombok.RequiredArgsConstructor;

/** Issues an access token and a stored refresh token; callers own the transaction. */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenRepository refreshTokens;
    private final SecretCodec codec;
    private final SessionPolicy policy;

    public Issued startFamily(UUID playerId, Instant now) {
        return issueInFamily(playerId, UUID.randomUUID(), now);
    }

    public Issued issueInFamily(UUID playerId, UUID familyId, Instant now) {
        String refreshValue = codec.newRefreshToken();
        RefreshToken stored = refreshTokens.save(
                RefreshToken.issue(playerId, familyId, codec.hash(refreshValue), now, policy.refreshTtl()));
        IssuedAccessToken access = accessTokens.issue(playerId, now);
        Session session = new Session(playerId, access.value(), access.expiresAt(), refreshValue,
                stored.getExpiresAt());
        return new Issued(session, stored.getId());
    }

    public record Issued(Session session, UUID refreshTokenId) {
    }
}
