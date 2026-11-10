package com.games.shipmaster.auth.domain;

import java.time.Instant;
import java.util.UUID;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(UUID playerId, Instant now);
}
