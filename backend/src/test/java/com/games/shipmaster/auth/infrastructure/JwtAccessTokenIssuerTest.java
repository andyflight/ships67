package com.games.shipmaster.auth.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.games.shipmaster.auth.domain.IssuedAccessToken;
import com.games.shipmaster.support.MutableClock;

class JwtAccessTokenIssuerTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC10ZXN0LW9ubHktc2VjcmV0LTEyMzQ1Njc4OTA=";

    private final AuthProperties props = new AuthProperties(SECRET, "shipmaster", Duration.ofMinutes(15),
            Duration.ofDays(60), Duration.ofSeconds(30));
    private final MutableClock clock = new MutableClock(Instant.now());
    private final AuthConfig config = new AuthConfig();
    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(config.jwtEncoder(props), props);
    private final JwtDecoder decoder = config.jwtDecoder(props, clock);

    @Test
    void issuedTokenDecodesWithExpectedClaims() {
        UUID player = UUID.randomUUID();
        Instant now = clock.instant();

        IssuedAccessToken token = issuer.issue(player, now);
        Jwt jwt = decoder.decode(token.value());

        assertThat(jwt.getSubject()).isEqualTo(player.toString());
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("shipmaster");
        assertThat(jwt.getExpiresAt()).isEqualTo(token.expiresAt());
        assertThat(token.expiresAt()).isEqualTo(now.truncatedTo(ChronoUnit.SECONDS).plus(Duration.ofMinutes(15)));
        assertThat(jwt.getId()).isNotBlank();
    }

    @Test
    void expiredTokenIsRejected() {
        IssuedAccessToken token = issuer.issue(UUID.randomUUID(), clock.instant());
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThatThrownBy(() -> decoder.decode(token.value())).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromOtherKeyIsRejected() {
        AuthProperties other = new AuthProperties("b3RoZXItc2VjcmV0LW90aGVyLXNlY3JldC1vdGhlci1zZWNyZXQ=",
                "shipmaster", Duration.ofMinutes(15), Duration.ofDays(60), Duration.ofSeconds(30));
        String foreign = new JwtAccessTokenIssuer(config.jwtEncoder(other), other)
                .issue(UUID.randomUUID(), clock.instant()).value();

        assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtException.class);
    }
}
