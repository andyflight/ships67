package com.games.shipmaster.common.api;

import java.net.URI;

/** Problem {@code type} URIs: stable identifiers for clients, not hosted pages. */
public final class ProblemTypes {

    private static final String BASE = "https://shipmaster.games/problems/";

    private ProblemTypes() {
    }

    public static URI of(String slug) {
        return URI.create(BASE + slug);
    }
}
