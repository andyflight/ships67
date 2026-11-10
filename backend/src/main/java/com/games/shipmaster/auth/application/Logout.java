package com.games.shipmaster.auth.application;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.auth.domain.RefreshTokenRepository;
import com.games.shipmaster.auth.domain.SecretCodec;

import lombok.RequiredArgsConstructor;

/** Ends the session family of the token. Unknown tokens are ignored so clients can always clear local state. */
@Service
@RequiredArgsConstructor
public class Logout {

    private final RefreshTokenRepository refreshTokens;
    private final SecretCodec codec;
    private final Clock clock;

    @Transactional
    public void execute(String refreshTokenValue) {
        refreshTokens.findByTokenHashForUpdate(codec.hash(refreshTokenValue))
                .ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), clock.instant()));
    }
}
