package com.queupid.pilago.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One clock for the whole app, set to Philippine time. Render's servers run on UTC, so never call
 * {@code LocalDate.now()} without this clock: inject {@link Clock} and use {@code LocalDate.now(clock)}.
 * Tests can replace it with {@code Clock.fixed(...)}.
 */
@Configuration
public class TimeConfig {

    public static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    @Bean
    public Clock clock() {
        return Clock.system(MANILA);
    }
}
