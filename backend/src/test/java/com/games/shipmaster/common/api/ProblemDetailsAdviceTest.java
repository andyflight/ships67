package com.games.shipmaster.common.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.games.shipmaster.common.domain.ConflictException;
import com.games.shipmaster.common.domain.InvalidInputException;
import com.games.shipmaster.common.domain.NotFoundException;
import com.games.shipmaster.common.domain.UnauthorizedException;
import com.games.shipmaster.support.ApiSliceConfig;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(controllers = ProblemDetailsAdviceTest.ThrowingController.class)
@Import({ ApiSliceConfig.class, ProblemDetailsAdviceTest.ThrowingController.class })
class ProblemDetailsAdviceTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @CsvSource({
            "not-found, 404",
            "conflict, 409",
            "invalid-input, 400",
            "unauthorized, 401",
    })
    void domainExceptionsBecomeProblems(String kind, int expectedStatus) throws Exception {
        mvc.perform(get("/test/throw/" + kind).with(jwt()))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://shipmaster.games/problems/" + kind))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value("boom"));
    }

    @Test
    void validationErrorsListFields() throws Exception {
        mvc.perform(post("/test/validate").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void unreadableBodyIsProblem() throws Exception {
        mvc.perform(post("/test/validate").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/throw/{kind}")
        String fail(@PathVariable String kind) {
            throw switch (kind) {
                case "not-found" -> new NotFoundException("boom");
                case "conflict" -> new ConflictException("boom");
                case "invalid-input" -> new InvalidInputException("boom");
                case "unauthorized" -> new UnauthorizedException("boom");
                default -> new IllegalArgumentException(UUID.randomUUID().toString());
            };
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody Body body) {
            return body.name();
        }

        record Body(@NotBlank String name) {
        }
    }
}
