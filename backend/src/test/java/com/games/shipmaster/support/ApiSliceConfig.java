package com.games.shipmaster.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

import com.games.shipmaster.auth.infrastructure.AuthConfig;
import com.games.shipmaster.common.api.SecurityConfig;
import com.games.shipmaster.common.api.SecurityProblemHandler;
import com.games.shipmaster.common.infrastructure.ClockConfig;

/** What a @WebMvcTest needs to run with the real security chain and JWT decoder. */
@TestConfiguration
@Import({ SecurityConfig.class, SecurityProblemHandler.class, AuthConfig.class, ClockConfig.class })
public class ApiSliceConfig {
}
