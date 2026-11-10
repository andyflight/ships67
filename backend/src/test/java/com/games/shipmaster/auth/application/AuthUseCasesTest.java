package com.games.shipmaster.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.games.shipmaster.auth.domain.AccessTokenIssuer;
import com.games.shipmaster.auth.domain.AuthProvider;
import com.games.shipmaster.auth.domain.IssuedAccessToken;
import com.games.shipmaster.auth.infrastructure.Sha256SecretCodec;
import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.player.application.CreateGuestPlayer;
import com.games.shipmaster.player.infrastructure.SecureRandomPlayerTagGenerator;
import com.games.shipmaster.support.InMemoryAuthIdentities;
import com.games.shipmaster.support.InMemoryPlayers;
import com.games.shipmaster.support.InMemoryRefreshTokens;
import com.games.shipmaster.support.MutableClock;

/** Use cases over in-memory ports and a fixed clock (design D4, D5). */
class AuthUseCasesTest {

    private final MutableClock clock = MutableClock.at("2026-11-10T12:00:00Z");
    private final InMemoryPlayers players = new InMemoryPlayers();
    private final InMemoryAuthIdentities identities = new InMemoryAuthIdentities();
    private final InMemoryRefreshTokens refreshTokens = new InMemoryRefreshTokens();
    private final Sha256SecretCodec codec = new Sha256SecretCodec();
    private final SessionPolicy policy = new SessionPolicy(Duration.ofDays(60), Duration.ofSeconds(30));
    private final AccessTokenIssuer accessTokens =
            (playerId, now) -> new IssuedAccessToken("access-" + UUID.randomUUID(), now.plusSeconds(900));
    private final SessionIssuer sessions = new SessionIssuer(accessTokens, refreshTokens, codec, policy);

    private final SignInAsGuest signIn = new SignInAsGuest(
            new CreateGuestPlayer(players, new SecureRandomPlayerTagGenerator(), clock), identities, codec, sessions,
            clock);
    private final RestoreGuestSession restore = new RestoreGuestSession(identities, codec, sessions, clock);
    private final RefreshSession refresh = new RefreshSession(refreshTokens, codec, sessions, policy, clock);
    private final Logout logout = new Logout(refreshTokens, codec, clock);

    @Test
    void guestSignInCreatesPlayerIdentityAndSession() {
        GuestSession g = signIn.execute();

        assertThat(players.byId).containsKey(g.session().playerId());
        assertThat(identities.findByPlayerId(g.session().playerId()))
                .singleElement().extracting(i -> i.getProvider()).isEqualTo(AuthProvider.GUEST);
        assertThat(g.guestCredential()).startsWith("g1.");
        // Only hashes are stored.
        assertThat(identities.all.getFirst().getCredentialHash()).isEqualTo(codec.hash(g.guestCredential()));
        assertThat(refreshTokens.byId.values()).singleElement()
                .extracting(t -> t.getTokenHash()).isEqualTo(codec.hash(g.session().refreshToken()));
        assertThat(g.session().refreshTokenExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(60)));
    }

    @Test
    void twoSignInsAreTwoPlayers() {
        assertThat(signIn.execute().session().playerId()).isNotEqualTo(signIn.execute().session().playerId());
    }

    @Test
    void refreshRotates() {
        Session first = signIn.execute().session();

        Session second = refresh.execute(first.refreshToken());

        assertThat(second.playerId()).isEqualTo(first.playerId());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(refresh.execute(second.refreshToken()).playerId()).isEqualTo(first.playerId());
    }

    @Test
    void retryWithinGraceReplacesUnusedSuccessor() {
        Session a = signIn.execute().session();
        Session b = refresh.execute(a.refreshToken());
        clock.advance(Duration.ofSeconds(5));

        Session c = refresh.execute(a.refreshToken());

        assertThat(c.playerId()).isEqualTo(a.playerId());
        assertThatThrownBy(() -> refresh.execute(b.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThat(refresh.execute(c.refreshToken()).playerId()).isEqualTo(a.playerId());
    }

    @Test
    void reuseAfterGraceRevokesFamily() {
        Session a = signIn.execute().session();
        Session b = refresh.execute(a.refreshToken());
        clock.advance(Duration.ofSeconds(30));

        assertThatThrownBy(() -> refresh.execute(a.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> refresh.execute(b.refreshToken())).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void reuseAfterSuccessorUsedRevokesFamily() {
        Session a = signIn.execute().session();
        Session b = refresh.execute(a.refreshToken());
        Session c = refresh.execute(b.refreshToken());

        assertThatThrownBy(() -> refresh.execute(a.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> refresh.execute(c.refreshToken())).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void expiredAndUnknownRefreshTokensAreRejected() {
        Session a = signIn.execute().session();
        clock.advance(Duration.ofDays(60));

        assertThatThrownBy(() -> refresh.execute(a.refreshToken())).isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> refresh.execute("never-issued")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void restoreAfterRevocationAndAfterExpiryKeepsPlayer() {
        GuestSession g = signIn.execute();
        Session b = refresh.execute(g.session().refreshToken());
        refresh.execute(b.refreshToken());
        assertThatThrownBy(() -> refresh.execute(g.session().refreshToken()))
                .isInstanceOf(UnauthorizedException.class); // family revoked

        assertThat(restore.execute(g.guestCredential()).playerId()).isEqualTo(g.session().playerId());

        clock.advance(Duration.ofDays(400));
        Session restored = restore.execute(g.guestCredential());
        assertThat(restored.playerId()).isEqualTo(g.session().playerId());
        assertThat(identities.all.getFirst().getLastUsedAt()).isEqualTo(clock.instant());
        assertThat(refresh.execute(restored.refreshToken()).playerId()).isEqualTo(g.session().playerId());
    }

    @Test
    void restoreWithUnknownCredentialIsRejected() {
        signIn.execute();
        assertThatThrownBy(() -> restore.execute("g1.unknown")).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void logoutRevokesFamilyButNotGuestCredential() {
        GuestSession g = signIn.execute();

        logout.execute(g.session().refreshToken());

        assertThatThrownBy(() -> refresh.execute(g.session().refreshToken()))
                .isInstanceOf(UnauthorizedException.class);
        assertThat(restore.execute(g.guestCredential()).playerId()).isEqualTo(g.session().playerId());
    }

    @Test
    void logoutWithUnknownTokenIsNoOp() {
        Session s = signIn.execute().session();

        logout.execute("never-issued");

        assertThat(refresh.execute(s.refreshToken()).playerId()).isEqualTo(s.playerId());
    }
}
