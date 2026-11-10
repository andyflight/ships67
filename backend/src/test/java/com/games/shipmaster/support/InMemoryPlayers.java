package com.games.shipmaster.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerRepository;

public final class InMemoryPlayers implements PlayerRepository {

    public final Map<UUID, Player> byId = new HashMap<>();

    @Override
    public Optional<Player> findById(UUID id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public boolean existsByTag(String tag) {
        return byId.values().stream().anyMatch(p -> p.getTag().value().equals(tag));
    }

    @Override
    public <S extends Player> S save(S player) {
        byId.put(player.getId(), player);
        return player;
    }
}
