package com.games.shipmaster.auth.application;

import java.time.Duration;

/** Refresh token lifetime and retry grace window, provided from configuration. */
public record SessionPolicy(Duration refreshTtl, Duration refreshGrace) {
}
