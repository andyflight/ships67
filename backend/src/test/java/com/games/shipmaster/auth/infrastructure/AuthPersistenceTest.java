package com.games.shipmaster.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.games.shipmaster.auth.domain.AuthIdentity;
import com.games.shipmaster.auth.domain.RefreshToken;
import com.games.shipmaster.player.domain.DisplayName;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerTag;
import com.games.shipmaster.player.infrastructure.PlayerJpaRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthPersistenceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    @Autowired
    PlayerJpaRepository players;

    @Autowired
    AuthIdentityJpaRepository identities;

    @Autowired
    RefreshTokenJpaRepository refreshTokens;

    @Autowired
    EntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    private UUID playerId;

    @BeforeEach
    void player() {
        playerId = UUID.randomUUID();
        players.saveAndFlush(Player.create(playerId, new PlayerTag("#8QK2LP9V"), new DisplayName("Sailor1"), NOW));
    }

    @Test
    void identityLookupByCredentialHashAndPlayer() {
        identities.saveAndFlush(AuthIdentity.guest(playerId, "a".repeat(64), NOW));
        em.clear();

        assertThat(identities.findByCredentialHash("a".repeat(64))).get()
                .extracting(AuthIdentity::getPlayerId).isEqualTo(playerId);
        assertThat(identities.findByCredentialHash("b".repeat(64))).isEmpty();
        assertThat(identities.findByPlayerId(playerId)).hasSize(1);
    }

    @Test
    void providerSubjectIsUnique() {
        identities.saveAndFlush(AuthIdentity.guest(playerId, "a".repeat(64), NOW));
        String subject = identities.findByPlayerId(playerId).getFirst().getSubject();

        assertThatThrownBy(() -> jdbc.update(
                "insert into auth_identity (id, player_id, provider, subject, created_at) values (?, ?, 'GUEST', ?, ?)",
                UUID.randomUUID(), playerId, subject, java.sql.Timestamp.from(NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void credentialHashIsUnique() {
        identities.saveAndFlush(AuthIdentity.guest(playerId, "a".repeat(64), NOW));

        assertThatThrownBy(() -> identities.saveAndFlush(AuthIdentity.guest(playerId, "a".repeat(64), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revokeFamilyRevokesOnlyThatFamily() {
        UUID family = UUID.randomUUID();
        UUID otherFamily = UUID.randomUUID();
        RefreshToken a = refreshTokens.save(RefreshToken.issue(playerId, family, "1".repeat(64), NOW, Duration.ofDays(1)));
        RefreshToken b = refreshTokens.save(RefreshToken.issue(playerId, family, "2".repeat(64), NOW, Duration.ofDays(1)));
        RefreshToken other = refreshTokens.save(
                RefreshToken.issue(playerId, otherFamily, "3".repeat(64), NOW, Duration.ofDays(1)));

        int revoked = refreshTokens.revokeFamily(family, NOW.plusSeconds(1));

        assertThat(revoked).isEqualTo(2);
        for (UUID id : List.of(a.getId(), b.getId())) {
            assertThat(refreshTokens.findById(id).orElseThrow().getRevokedAt()).isEqualTo(NOW.plusSeconds(1));
        }
        assertThat(refreshTokens.findById(other.getId()).orElseThrow().isRevoked()).isFalse();
        assertThat(refreshTokens.findByTokenHashForUpdate("1".repeat(64))).isPresent();
    }

    @Test
    void deletingPlayerCascades() {
        identities.saveAndFlush(AuthIdentity.guest(playerId, "a".repeat(64), NOW));
        refreshTokens.saveAndFlush(RefreshToken.issue(playerId, UUID.randomUUID(), "1".repeat(64), NOW, Duration.ofDays(1)));
        em.clear();

        jdbc.update("delete from player where id = ?", playerId);

        assertThat(identities.findByPlayerId(playerId)).isEmpty();
        assertThat(refreshTokens.findByTokenHashForUpdate("1".repeat(64))).isEmpty();
    }
}
