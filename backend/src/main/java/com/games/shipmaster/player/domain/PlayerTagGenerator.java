package com.games.shipmaster.player.domain;

public interface PlayerTagGenerator {

    /** A random, well-formed tag; uniqueness is checked by the caller. */
    PlayerTag next();
}
