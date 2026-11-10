package com.games.shipmaster.player.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetPlayerProfile {

    private final PlayerRepository players;
    private final PlayerProvidersLookup providers;

    @Transactional(readOnly = true)
    public PlayerProfile execute(UUID playerId) {
        Player player = players.findById(playerId)
                // A valid token for a removed player: treat as not signed in, the client must sign in again.
                .orElseThrow(() -> new UnauthorizedException("Player no longer exists"));
        return toProfile(player, providers);
    }

    static PlayerProfile toProfile(Player player, PlayerProvidersLookup providers) {
        return new PlayerProfile(
                player.getId(),
                player.getTag().value(),
                player.getDisplayName().value(),
                player.getCreatedAt(),
                providers.providersOf(player.getId()));
    }
}
