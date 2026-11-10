package com.games.shipmaster.player.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.games.shipmaster.common.domain.InvalidInputException;
import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.player.application.GetPlayerProfile;
import com.games.shipmaster.player.application.PlayerProfile;
import com.games.shipmaster.player.application.RenamePlayer;
import com.games.shipmaster.support.ApiSliceConfig;

/** HTTP contract of specs/player-account. */
@WebMvcTest(PlayerController.class)
@Import(ApiSliceConfig.class)
class PlayerControllerTest {

    private static final UUID PLAYER = UUID.fromString("3f1c2a4e-0000-4000-8000-000000000001");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GetPlayerProfile getPlayerProfile;

    @MockitoBean
    RenamePlayer renamePlayer;

    private static PlayerProfile profile(String name) {
        return new PlayerProfile(PLAYER, "#8QK2LP9V", name, Instant.parse("2026-11-10T12:00:00Z"), List.of("GUEST"));
    }

    @Test
    void getProfile() throws Exception {
        when(getPlayerProfile.execute(PLAYER)).thenReturn(profile("Sailor4821"));

        mvc.perform(get("/api/v1/players/me").with(jwt().jwt(j -> j.subject(PLAYER.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(PLAYER.toString()))
                .andExpect(jsonPath("$.tag").value("#8QK2LP9V"))
                .andExpect(jsonPath("$.displayName").value("Sailor4821"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-10T12:00:00Z"))
                .andExpect(jsonPath("$.providers[0]").value("GUEST"));
    }

    @Test
    void removedPlayerIs401() throws Exception {
        when(getPlayerProfile.execute(PLAYER)).thenThrow(new UnauthorizedException("Player no longer exists"));

        mvc.perform(get("/api/v1/players/me").with(jwt().jwt(j -> j.subject(PLAYER.toString()))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void nonUuidSubjectIs401() throws Exception {
        mvc.perform(get("/api/v1/players/me").with(jwt().jwt(j -> j.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rename() throws Exception {
        when(renamePlayer.execute(PLAYER, "Captain Hook")).thenReturn(profile("Captain Hook"));

        mvc.perform(patch("/api/v1/players/me").with(jwt().jwt(j -> j.subject(PLAYER.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Captain Hook\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Captain Hook"));
    }

    @Test
    void invalidNameIs400() throws Exception {
        when(renamePlayer.execute(eq(PLAYER), any())).thenThrow(new InvalidInputException("bad name"));

        mvc.perform(patch("/api/v1/players/me").with(jwt().jwt(j -> j.subject(PLAYER.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void missingNameIs400() throws Exception {
        mvc.perform(patch("/api/v1/players/me").with(jwt().jwt(j -> j.subject(PLAYER.toString())))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
