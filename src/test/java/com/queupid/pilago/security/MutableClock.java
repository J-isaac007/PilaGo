package com.queupid.pilago.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import com.queupid.pilago.config.TimeConfig;

/** A clock for tests: time stands still until the test moves it forward. */
public class MutableClock extends Clock {

    private Instant now = Instant.parse("2026-11-16T01:00:00Z"); // 9:00 AM in Manila

    public void plus(Duration amount) {
        now = now.plus(amount);
    }

    @Override
    public ZoneId getZone() {
        return TimeConfig.MANILA;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
