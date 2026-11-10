package com.games.shipmaster.player.api.dto;

import jakarta.validation.constraints.NotNull;

/** Name rules are enforced by the domain (DisplayName); here only presence. */
public record RenamePlayerRequest(@NotNull String displayName) {
}
