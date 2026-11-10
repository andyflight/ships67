package com.games.shipmaster.player.domain;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository {

    Optional<Player> findById(UUID id);

    boolean existsByTag(String tag);

    <S extends Player> S save(S player);
}
