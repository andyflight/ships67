package com.games.shipmaster.player.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.games.shipmaster.common.domain.InvalidInputException;
import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.player.domain.DisplayName;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerTag;
import com.games.shipmaster.player.domain.PlayerTagGenerator;
import com.games.shipmaster.support.InMemoryPlayers;
import com.games.shipmaster.support.MutableClock;

class PlayerUseCasesTest {

    private final MutableClock clock = MutableClock.at("2026-11-10T12:00:00Z");
    private final InMemoryPlayers players = new InMemoryPlayers();
    private final PlayerProvidersLookup providers = id -> List.of("GUEST");

    @Test
    void createGuestPlayerHasTagAndDefaultName() {
        UUID id = new CreateGuestPlayer(players, () -> new PlayerTag("#8QK2LP9V"), clock).execute();

        Player player = players.byId.get(id);
        assertThat(player.getTag().value()).isEqualTo("#8QK2LP9V");
        assertThat(player.getDisplayName().value()).matches("^Sailor\\d{4}$");
        assertThat(player.getCreatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void createRetriesOnTagCollision() {
        players.save(Player.create(UUID.randomUUID(), new PlayerTag("#22222222"), new DisplayName("Taken"),
                clock.instant()));
        Deque<String> tags = new ArrayDeque<>(List.of("#22222222", "#22222222", "#33333333"));
        PlayerTagGenerator generator = () -> new PlayerTag(tags.pop());

        UUID id = new CreateGuestPlayer(players, generator, clock).execute();

        assertThat(players.byId.get(id).getTag().value()).isEqualTo("#33333333");
    }

    @Test
    void createGivesUpAfterMaxAttempts() {
        players.save(Player.create(UUID.randomUUID(), new PlayerTag("#22222222"), new DisplayName("Taken"),
                clock.instant()));

        assertThatThrownBy(() -> new CreateGuestPlayer(players, () -> new PlayerTag("#22222222"), clock).execute())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getProfileReturnsProfileWithProviders() {
        UUID id = new CreateGuestPlayer(players, () -> new PlayerTag("#8QK2LP9V"), clock).execute();

        PlayerProfile profile = new GetPlayerProfile(players, providers).execute(id);

        assertThat(profile.id()).isEqualTo(id);
        assertThat(profile.tag()).isEqualTo("#8QK2LP9V");
        assertThat(profile.providers()).containsExactly("GUEST");
        assertThat(profile.createdAt()).isEqualTo(clock.instant());
    }

    @Test
    void unknownPlayerIsUnauthorized() {
        assertThatThrownBy(() -> new GetPlayerProfile(players, providers).execute(UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class);
        assertThatThrownBy(() -> new RenamePlayer(players, providers).execute(UUID.randomUUID(), "Captain"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void renameTrimsAndValidates() {
        UUID id = new CreateGuestPlayer(players, () -> new PlayerTag("#8QK2LP9V"), clock).execute();
        RenamePlayer rename = new RenamePlayer(players, providers);

        assertThat(rename.execute(id, "  Captain Hook ").displayName()).isEqualTo("Captain Hook");
        assertThatThrownBy(() -> rename.execute(id, "x")).isInstanceOf(InvalidInputException.class);
        assertThat(players.byId.get(id).getDisplayName().value()).isEqualTo("Captain Hook");
    }
}
