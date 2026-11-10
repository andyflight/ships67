package com.games.shipmaster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** The whole guest lifecycle through the real stack (security, use cases, Liquibase schema, H2). */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void guestLifecycle() throws Exception {
        // Sign in
        String guest = body(mvc.perform(post("/api/v1/auth/guest")).andExpect(status().isCreated()));
        String playerId = JsonPath.read(guest, "$.playerId");
        String credential = JsonPath.read(guest, "$.guestCredential");
        String access = JsonPath.read(guest, "$.accessToken");
        String refreshA = JsonPath.read(guest, "$.refreshToken");

        // Profile + rename
        mvc.perform(get("/api/v1/players/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playerId))
                .andExpect(jsonPath("$.tag").value(org.hamcrest.Matchers.matchesPattern("^#[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{8}$")))
                .andExpect(jsonPath("$.displayName").value(org.hamcrest.Matchers.matchesPattern("^Sailor\\d{4}$")))
                .andExpect(jsonPath("$.providers[0]").value("GUEST"));
        mvc.perform(patch("/api/v1/players/me").header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"Captain Hook\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Captain Hook"));

        // Refresh A -> B, then retry A within the grace window (lost response) -> C, B is dropped
        refresh(refreshA).andExpect(status().isOk());
        String refreshC = JsonPath.read(body(refresh(refreshA).andExpect(status().isOk())), "$.refreshToken");

        // Use C -> D; replaying A now is theft: 401 and the whole family dies
        String refreshD = JsonPath.read(body(refresh(refreshC).andExpect(status().isOk())), "$.refreshToken");
        refresh(refreshA).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        refresh(refreshD).andExpect(status().isUnauthorized());

        // Guest credential restores the same, renamed player
        String restored = body(restore(credential).andExpect(status().isOk()));
        assertThat((String) JsonPath.read(restored, "$.playerId")).isEqualTo(playerId);
        mvc.perform(get("/api/v1/players/me")
                        .header("Authorization", "Bearer " + JsonPath.read(restored, "$.accessToken")))
                .andExpect(jsonPath("$.displayName").value("Captain Hook"));

        // Logout kills the session, not the account
        String restoredRefresh = JsonPath.read(restored, "$.refreshToken");
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + restoredRefresh + "\"}"))
                .andExpect(status().isNoContent());
        refresh(restoredRefresh).andExpect(status().isUnauthorized());
        assertThat((String) JsonPath.read(body(restore(credential).andExpect(status().isOk())), "$.playerId"))
                .isEqualTo(playerId);

        // A second guest is a different player
        String other = body(mvc.perform(post("/api/v1/auth/guest")).andExpect(status().isCreated()));
        assertThat((String) JsonPath.read(other, "$.playerId")).isNotEqualTo(playerId);
    }

    private ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + token + "\"}"));
    }

    private ResultActions restore(String credential) throws Exception {
        return mvc.perform(post("/api/v1/auth/guest/restore").contentType(MediaType.APPLICATION_JSON)
                .content("{\"guestCredential\":\"" + credential + "\"}"));
    }

    private static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
