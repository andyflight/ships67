package com.games.shipmaster.player.application;

import java.util.List;
import java.util.UUID;

/**
 * Linked sign-in providers of a player. Implemented by the auth domain, so player does not depend on auth.
 */
public interface PlayerProvidersLookup {

    List<String> providersOf(UUID playerId);
}
