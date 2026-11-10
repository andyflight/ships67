package com.games.shipmaster.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/** Every branch of the refresh decision table (design D4). */
class RefreshTokenTest {

    private static final Instant T0 = Instant.parse("2026-11-10T12:00:00Z");
    private static final Duration TTL = Duration.ofDays(60);
    private static final Duration GRACE = Duration.ofSeconds(30);

    private final UUID player = UUID.randomUUID();
    private final UUID family = UUID.randomUUID();

    private RefreshToken token(Instant at) {
        return RefreshToken.issue(player, family, UUID.randomUUID().toString(), at, TTL);
    }

    @Test
    void unusedTokenRotates() {
        assertThat(token(T0).decide(T0.plusSeconds(1), GRACE, null)).isEqualTo(RefreshDecision.ROTATE);
    }

    @Test
    void expiredTokenIsInvalid() {
        RefreshToken a = token(T0);
        assertThat(a.decide(T0.plus(TTL), GRACE, null)).isEqualTo(RefreshDecision.INVALID);
        assertThat(a.decide(T0.plus(TTL).minusSeconds(1), GRACE, null)).isEqualTo(RefreshDecision.ROTATE);
    }

    @Test
    void revokedTokenIsInvalid() {
        RefreshToken a = token(T0);
        a.revoke(T0);
        assertThat(a.decide(T0.plusSeconds(1), GRACE, null)).isEqualTo(RefreshDecision.INVALID);
    }

    @Test
    void retryWithinGraceWithUntouchedSuccessor() {
        RefreshToken a = token(T0);
        RefreshToken b = token(T0);
        a.rotatedTo(b.getId(), T0);

        assertThat(a.decide(T0.plusSeconds(5), GRACE, b)).isEqualTo(RefreshDecision.RETRY_IN_GRACE);
    }

    @Test
    void graceBoundaryIsExclusiveAt30Seconds() {
        RefreshToken a = token(T0);
        RefreshToken b = token(T0);
        a.rotatedTo(b.getId(), T0);

        assertThat(a.decide(T0.plus(GRACE).minusMillis(1), GRACE, b)).isEqualTo(RefreshDecision.RETRY_IN_GRACE);
        assertThat(a.decide(T0.plus(GRACE), GRACE, b)).isEqualTo(RefreshDecision.REUSE_DETECTED);
    }

    @Test
    void reuseAfterSuccessorWasUsed() {
        RefreshToken a = token(T0);
        RefreshToken b = token(T0);
        RefreshToken c = token(T0);
        a.rotatedTo(b.getId(), T0);
        b.rotatedTo(c.getId(), T0.plusSeconds(1));

        assertThat(a.decide(T0.plusSeconds(2), GRACE, b)).isEqualTo(RefreshDecision.REUSE_DETECTED);
    }

    @Test
    void reuseWhenSuccessorRevokedOrMissing() {
        RefreshToken a = token(T0);
        RefreshToken b = token(T0);
        a.rotatedTo(b.getId(), T0);
        b.revoke(T0);

        assertThat(a.decide(T0.plusSeconds(1), GRACE, b)).isEqualTo(RefreshDecision.REUSE_DETECTED);
        assertThat(a.decide(T0.plusSeconds(1), GRACE, null)).isEqualTo(RefreshDecision.REUSE_DETECTED);
    }

    @Test
    void graceAnchorsOnFirstUse() {
        RefreshToken a = token(T0);
        a.rotatedTo(UUID.randomUUID(), T0);
        a.rotatedTo(UUID.randomUUID(), T0.plusSeconds(20));

        assertThat(a.getUsedAt()).isEqualTo(T0);
    }
}
