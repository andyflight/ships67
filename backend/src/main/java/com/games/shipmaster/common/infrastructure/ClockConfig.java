package com.games.shipmaster.common.infrastructure;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    /** All time reads go through this bean so tests can move time instead of sleeping. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
