package com.games.shipmaster.archfixture.bad.domain;

import org.springframework.stereotype.Component;

/** Fixture: violates DOMAIN_IS_PURE (Spring in domain). Never used at runtime. */
@Component
public class SpringAwareEntity {
}
