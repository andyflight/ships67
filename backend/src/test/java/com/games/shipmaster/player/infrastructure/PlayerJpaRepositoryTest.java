package com.games.shipmaster.player.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import com.games.shipmaster.player.domain.DisplayName;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerTag;

import jakarta.persistence.EntityManager;

/** Runs on the Liquibase schema (ddl-auto=validate), so it also checks entity/schema agreement. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PlayerJpaRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-11-10T12:00:00Z");

    @Autowired
    PlayerJpaRepository players;

    @Autowired
    EntityManager em;

    @Test
    void roundTrip() {
        UUID id = UUID.randomUUID();
        players.save(Player.create(id, new PlayerTag("#8QK2LP9V"), new DisplayName("Капітан"), NOW));
        em.flush();
        em.clear();

        Player loaded = players.findById(id).orElseThrow();

        assertThat(loaded.getTag().value()).isEqualTo("#8QK2LP9V");
        assertThat(loaded.getDisplayName().value()).isEqualTo("Капітан");
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
        assertThat(players.existsByTag("#8QK2LP9V")).isTrue();
        assertThat(players.existsByTag("#22222222")).isFalse();
    }

    @Test
    void duplicateTagViolatesUniqueConstraint() {
        players.saveAndFlush(Player.create(UUID.randomUUID(), new PlayerTag("#8QK2LP9V"), new DisplayName("One"), NOW));

        assertThatThrownBy(() -> players.saveAndFlush(
                Player.create(UUID.randomUUID(), new PlayerTag("#8QK2LP9V"), new DisplayName("Two"), NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
