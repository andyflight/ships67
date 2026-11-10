package com.games.shipmaster.player.api;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.games.shipmaster.common.api.CurrentPlayer;
import com.games.shipmaster.player.api.dto.PlayerProfileResponse;
import com.games.shipmaster.player.api.dto.RenamePlayerRequest;
import com.games.shipmaster.player.application.GetPlayerProfile;
import com.games.shipmaster.player.application.RenamePlayer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/players/me")
@RequiredArgsConstructor
public class PlayerController {

    private final GetPlayerProfile getPlayerProfile;
    private final RenamePlayer renamePlayer;

    @GetMapping
    public PlayerProfileResponse me(@CurrentPlayer UUID playerId) {
        return PlayerProfileResponse.from(getPlayerProfile.execute(playerId));
    }

    @PatchMapping
    public PlayerProfileResponse rename(@CurrentPlayer UUID playerId, @Valid @RequestBody RenamePlayerRequest request) {
        return PlayerProfileResponse.from(renamePlayer.execute(playerId, request.displayName()));
    }
}
