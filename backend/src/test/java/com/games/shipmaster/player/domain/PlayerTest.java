package com.games.shipmaster.player.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void createAndRename() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-10T12:00:00Z");
        Player player = Player.create(id, new PlayerTag("#8QK2LP9V"), new DisplayName("Sailor1234"), now);

        player.rename(new DisplayName("Captain Hook"));

        assertThat(player.getId()).isEqualTo(id);
        assertThat(player.getTag().value()).isEqualTo("#8QK2LP9V");
        assertThat(player.getDisplayName().value()).isEqualTo("Captain Hook");
        assertThat(player.getCreatedAt()).isEqualTo(now);
    }
}
