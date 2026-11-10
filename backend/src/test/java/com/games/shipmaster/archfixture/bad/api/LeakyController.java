package com.games.shipmaster.archfixture.bad.api;

import com.games.shipmaster.archfixture.bad.domain.SpringAwareEntity;

/** Fixture: violates API_USES_APPLICATION_ONLY (returns a domain entity). */
public class LeakyController {

    public SpringAwareEntity get() {
        return new SpringAwareEntity();
    }
}
