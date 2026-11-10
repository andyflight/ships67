package com.games.shipmaster.player.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerRepository;

/**
 * Spring Data implements the domain port directly; no adapter class needed. Methods declared by both the port and
 * CrudRepository are redeclared here so callers of this interface are not ambiguous.
 */
public interface PlayerJpaRepository extends JpaRepository<Player, UUID>, PlayerRepository {

    @Override
    <S extends Player> S save(S player);

    @Override
    Optional<Player> findById(UUID id);
}
