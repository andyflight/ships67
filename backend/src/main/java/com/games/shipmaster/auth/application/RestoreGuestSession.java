package com.games.shipmaster.auth.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.auth.domain.AuthIdentity;
import com.games.shipmaster.auth.domain.AuthIdentityRepository;
import com.games.shipmaster.auth.domain.AuthProvider;
import com.games.shipmaster.auth.domain.SecretCodec;
import com.games.shipmaster.common.domain.UnauthorizedException;

import lombok.RequiredArgsConstructor;

/** Account-loss guard: the guest credential starts a new session regardless of refresh token state. */
@Service
@RequiredArgsConstructor
public class RestoreGuestSession {

    private final AuthIdentityRepository identities;
    private final SecretCodec codec;
    private final SessionIssuer sessions;
    private final Clock clock;

    @Transactional
    public Session execute(String guestCredential) {
        Instant now = clock.instant();
        AuthIdentity identity = identities.findByCredentialHash(codec.hash(guestCredential))
                .filter(i -> i.getProvider() == AuthProvider.GUEST)
                .orElseThrow(() -> new UnauthorizedException("Unknown guest credential"));
        identity.markUsed(now);
        identities.save(identity);
        return sessions.startFamily(identity.getPlayerId(), now).session();
    }
}
