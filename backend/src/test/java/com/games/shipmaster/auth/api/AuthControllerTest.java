package com.games.shipmaster.auth.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.games.shipmaster.auth.application.GuestSession;
import com.games.shipmaster.auth.application.Logout;
import com.games.shipmaster.auth.application.RefreshSession;
import com.games.shipmaster.auth.application.RestoreGuestSession;
import com.games.shipmaster.auth.application.Session;
import com.games.shipmaster.auth.application.SignInAsGuest;
import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.support.ApiSliceConfig;

/** HTTP contract of specs/player-auth. */
@WebMvcTest(AuthController.class)
@Import(ApiSliceConfig.class)
class AuthControllerTest {

    private static final UUID PLAYER = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");
    private static final Session SESSION = new Session(PLAYER, "eyJ.access", Instant.parse("2026-11-10T12:15:00Z"),
            "q8Zk-refresh", Instant.parse("2027-01-09T12:00:00Z"));

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SignInAsGuest signInAsGuest;

    @MockitoBean
    RestoreGuestSession restoreGuestSession;

    @MockitoBean
    RefreshSession refreshSession;

    @MockitoBean
    Logout logout;

    @Test
    void guestReturns201WithSessionAndCredential() throws Exception {
        when(signInAsGuest.execute()).thenReturn(new GuestSession(SESSION, "g1.secret"));

        mvc.perform(post("/api/v1/auth/guest"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.playerId").value(PLAYER.toString()))
                .andExpect(jsonPath("$.accessToken").value("eyJ.access"))
                .andExpect(jsonPath("$.accessTokenExpiresAt").value("2026-11-10T12:15:00Z"))
                .andExpect(jsonPath("$.refreshToken").value("q8Zk-refresh"))
                .andExpect(jsonPath("$.refreshTokenExpiresAt").value("2027-01-09T12:00:00Z"))
                .andExpect(jsonPath("$.guestCredential").value("g1.secret"));
    }

    @Test
    void restoreReturns200Session() throws Exception {
        when(restoreGuestSession.execute("g1.secret")).thenReturn(SESSION);

        mvc.perform(post("/api/v1/auth/guest/restore").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestCredential\":\"g1.secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(PLAYER.toString()))
                .andExpect(jsonPath("$.guestCredential").doesNotExist());
    }

    @Test
    void restoreUnknownIs401() throws Exception {
        when(restoreGuestSession.execute(anyString())).thenThrow(new UnauthorizedException("Unknown guest credential"));

        mvc.perform(post("/api/v1/auth/guest/restore").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestCredential\":\"g1.nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void restoreMissingFieldIs400() throws Exception {
        mvc.perform(post("/api/v1/auth/guest/restore").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("guestCredential"));
    }

    @Test
    void refreshReturns200Session() throws Exception {
        when(refreshSession.execute("q8Zk-refresh")).thenReturn(SESSION);

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"q8Zk-refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("q8Zk-refresh"));
    }

    @Test
    void refreshInvalidIs401() throws Exception {
        when(refreshSession.execute(anyString())).thenThrow(new UnauthorizedException("Invalid refresh token"));

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"old\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void refreshMissingFieldIs400() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void logoutReturns204() throws Exception {
        doNothing().when(logout).execute("any-token");

        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"any-token\"}"))
                .andExpect(status().isNoContent());
        verify(logout).execute("any-token");
    }

    @Test
    void logoutMissingFieldIs400() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
