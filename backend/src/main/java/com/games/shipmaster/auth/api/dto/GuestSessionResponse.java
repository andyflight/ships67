package com.games.shipmaster.auth.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.games.shipmaster.auth.application.GuestSession;

/** Session fields plus the guest credential; the only response that ever contains the credential. */
public record GuestSessionResponse(UUID playerId, String accessToken, Instant accessTokenExpiresAt,
        String refreshToken, Instant refreshTokenExpiresAt, String guestCredential) {

    public static GuestSessionResponse from(GuestSession g) {
        var s = g.session();
        return new GuestSessionResponse(s.playerId(), s.accessToken(), s.accessTokenExpiresAt(), s.refreshToken(),
                s.refreshTokenExpiresAt(), g.guestCredential());
    }
}
