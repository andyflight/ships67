package com.games.shipmaster.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.auth.domain.AuthIdentity;
import com.games.shipmaster.auth.domain.AuthIdentityRepository;
import com.games.shipmaster.auth.domain.SecretCodec;
import com.games.shipmaster.player.application.CreateGuestPlayer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignInAsGuest {

    private final CreateGuestPlayer createGuestPlayer;
    private final AuthIdentityRepository identities;
    private final SecretCodec codec;
    private final SessionIssuer sessions;
    private final Clock clock;

    @Transactional
    public GuestSession execute() {
        Instant now = clock.instant();
        UUID playerId = createGuestPlayer.execute();
        String credential = codec.newGuestCredential();
        identities.save(AuthIdentity.guest(playerId, codec.hash(credential), now));
        return new GuestSession(sessions.startFamily(playerId, now).session(), credential);
    }
}
