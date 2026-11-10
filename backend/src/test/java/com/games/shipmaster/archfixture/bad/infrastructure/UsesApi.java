package com.games.shipmaster.archfixture.bad.infrastructure;

import com.games.shipmaster.archfixture.bad.api.LeakyController;

/** Fixture: violates NOTHING_DEPENDS_ON_API. */
public class UsesApi {

    public Object call() {
        return new LeakyController().get();
    }
}
