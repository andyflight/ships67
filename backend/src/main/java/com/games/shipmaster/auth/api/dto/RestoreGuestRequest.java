package com.games.shipmaster.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RestoreGuestRequest(@NotBlank String guestCredential) {
}
