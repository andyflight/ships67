package com.games.shipmaster.player.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.player.domain.DisplayName;
import com.games.shipmaster.player.domain.Player;
import com.games.shipmaster.player.domain.PlayerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RenamePlayer {

    private final PlayerRepository players;
    private final PlayerProvidersLookup providers;

    @Transactional
    public PlayerProfile execute(UUID playerId, String newName) {
        DisplayName name = new DisplayName(newName);
        Player player = players.findById(playerId)
                .orElseThrow(() -> new UnauthorizedException("Player no longer exists"));
        player.rename(name);
        return GetPlayerProfile.toProfile(players.save(player), providers);
    }
}
