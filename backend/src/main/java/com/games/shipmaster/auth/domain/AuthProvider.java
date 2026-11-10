package com.games.shipmaster.auth.domain;

/** Sign-in providers. Only GUEST has code paths; the rest are reserved for account linking. */
public enum AuthProvider {
    GUEST,
    GAME_CENTER,
    PLAY_GAMES,
    APPLE
}
