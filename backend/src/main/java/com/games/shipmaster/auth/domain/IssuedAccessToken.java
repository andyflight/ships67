package com.games.shipmaster.auth.domain;

import java.time.Instant;

public record IssuedAccessToken(String value, Instant expiresAt) {
}
