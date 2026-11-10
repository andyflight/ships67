package com.games.shipmaster.auth.application;

import java.time.Instant;
import java.util.UUID;

public record Session(UUID playerId, String accessToken, Instant accessTokenExpiresAt, String refreshToken,
        Instant refreshTokenExpiresAt) {
}
