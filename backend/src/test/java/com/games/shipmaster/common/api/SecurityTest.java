package com.games.shipmaster.common.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.games.shipmaster.auth.api.AuthController;
import com.games.shipmaster.auth.application.GuestSession;
import com.games.shipmaster.auth.application.Logout;
import com.games.shipmaster.auth.application.Session;
import com.games.shipmaster.auth.application.RefreshSession;
import com.games.shipmaster.auth.application.RestoreGuestSession;
import com.games.shipmaster.auth.application.SignInAsGuest;
import com.games.shipmaster.auth.infrastructure.JwtAccessTokenIssuer;
import com.games.shipmaster.player.api.PlayerController;
import com.games.shipmaster.player.application.GetPlayerProfile;
import com.games.shipmaster.player.application.PlayerProfile;
import com.games.shipmaster.player.application.RenamePlayer;
import com.games.shipmaster.support.ApiSliceConfig;
import com.games.shipmaster.support.MutableClock;

/** Real filter chain and real JWTs: protected-by-default, public auth paths, 401 as problem+json. */
@WebMvcTest(controllers = { PlayerController.class, AuthController.class })
@Import({ ApiSliceConfig.class, JwtAccessTokenIssuer.class, SecurityTest.ClockOverride.class })
class SecurityTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JwtAccessTokenIssuer issuer;

    @Autowired
    MutableClock clock;

    @MockitoBean
    GetPlayerProfile getPlayerProfile;

    @MockitoBean
    RenamePlayer renamePlayer;

    @MockitoBean
    SignInAsGuest signInAsGuest;

    @MockitoBean
    RestoreGuestSession restoreGuestSession;

    @MockitoBean
    RefreshSession refreshSession;

    @MockitoBean
    Logout logout;

    private final UUID playerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        clock.set(Instant.now());
        when(getPlayerProfile.execute(any())).thenAnswer(inv -> new PlayerProfile(inv.getArgument(0), "#8QK2LP9V",
                "Sailor1234", Instant.parse("2026-11-10T12:00:00Z"), List.of("GUEST")));
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        String token = issuer.issue(playerId, clock.instant()).value();

        mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerId.toString()));
    }

    @Test
    void missingTokenIs401Problem() throws Exception {
        mvc.perform(get("/api/v1/players/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("https://shipmaster.games/problems/unauthorized"));
    }

    @Test
    void tamperedTokenIs401Problem() throws Exception {
        String[] mine = issuer.issue(playerId, clock.instant()).value().split("\\.");
        String[] other = issuer.issue(UUID.randomUUID(), clock.instant()).value().split("\\.");
        String forged = mine[0] + "." + other[1] + "." + mine[2];

        mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void expiredTokenIs401Problem() throws Exception {
        String token = issuer.issue(playerId, clock.instant()).value();
        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void unknownApiPathNeedsToken() throws Exception {
        mvc.perform(get("/api/v1/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void publicAuthPathsNeedNoToken() throws Exception {
        Session session = new Session(playerId, "a", clock.instant(), "r", clock.instant());
        when(signInAsGuest.execute()).thenReturn(new GuestSession(session, "g1.x"));
        when(restoreGuestSession.execute(any())).thenReturn(session);
        when(refreshSession.execute(any())).thenReturn(session);
        String body = "{\"refreshToken\":\"x\",\"guestCredential\":\"g1.x\"}";
        for (String path : SecurityConfig.PUBLIC_AUTH_ENDPOINTS) {
            int code = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
            org.assertj.core.api.Assertions.assertThat(code).as(path).isBetween(200, 299);
        }
    }

    @TestConfiguration
    static class ClockOverride {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now());
        }
    }
}
