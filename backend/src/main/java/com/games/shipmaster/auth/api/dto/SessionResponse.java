package com.games.shipmaster.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.games.shipmaster.auth.application.Session;

public record SessionResponse(UUID playerId, String accessToken, Instant accessTokenExpiresAt, String refreshToken,
        Instant refreshTokenExpiresAt) {

    public static SessionResponse from(Session s) {
        return new SessionResponse(s.playerId(), s.accessToken(), s.accessTokenExpiresAt(), s.refreshToken(),
                s.refreshTokenExpiresAt());
    }
}
