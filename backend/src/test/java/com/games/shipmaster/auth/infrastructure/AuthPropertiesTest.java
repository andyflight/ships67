package com.games.shipmaster.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AuthPropertiesTest {

    private static final String VALID_SECRET = "dGVzdC1vbmx5LXNlY3JldC10ZXN0LW9ubHktc2VjcmV0LTEyMzQ1Njc4OTA=";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropsConfig.class)
            .withPropertyValues(
                    "shipmaster.auth.issuer=shipmaster",
                    "shipmaster.auth.access-ttl=15m",
                    "shipmaster.auth.refresh-ttl=60d",
                    "shipmaster.auth.refresh-grace=30s");

    @Test
    void validSecretStarts() {
        runner.withPropertyValues("shipmaster.auth.jwt-secret=" + VALID_SECRET)
                .run(ctx -> assertThat(ctx).hasNotFailed().hasSingleBean(AuthProperties.class));
    }

    @Test
    void missingSecretFailsStartup() {
        runner.withPropertyValues("shipmaster.auth.jwt-secret=")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("SHIPMASTER_JWT_SECRET"));
    }

    @Test
    void shortSecretFailsStartup() {
        // base64 of 16 bytes
        runner.withPropertyValues("shipmaster.auth.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZg==")
                .run(ctx -> assertThat(ctx).hasFailed()
                        .getFailure().rootCause().hasMessageContaining("at least 32 bytes"));
    }

    @Configuration
    @EnableConfigurationProperties(AuthProperties.class)
    static class PropsConfig {
    }
}
