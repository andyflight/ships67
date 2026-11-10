package com.games.shipmaster.player.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.games.shipmaster.player.application.PlayerProfile;

public record PlayerProfileResponse(UUID id, String tag, String displayName, Instant createdAt, List<String> providers) {

    public static PlayerProfileResponse from(PlayerProfile p) {
        return new PlayerProfileResponse(p.id(), p.tag(), p.displayName(), p.createdAt(), p.providers());
    }
}
