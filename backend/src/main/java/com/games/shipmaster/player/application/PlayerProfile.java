package com.games.shipmaster.player.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlayerProfile(UUID id, String tag, String displayName, Instant createdAt, List<String> providers) {
}
