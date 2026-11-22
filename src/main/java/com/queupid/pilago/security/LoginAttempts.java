package com.queupid.pilago.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Counts wrong passwords per Staff ID. After {@link #MAX_FAILURES} in a row the Staff ID is locked for
 * {@link #LOCK_TIME}. Encapsulation: the counters are private, and the only way to change them is
 * {@link #recordFailure}, {@link #recordSuccess} and the lock ending by itself. It reads time from the
 * app's {@link Clock}, so tests can move time forward.
 */
@Component
public class LoginAttempts {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(10);

    /** One Staff ID's counter. */
    private static final class Counter {
        private int failures;
        private Instant lockedUntil;
    }

    private final Clock clock;
    private final Map<String, Counter> records = new HashMap<>();

    public LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** A wrong password. The 5th one in a row starts the lock. */
    public synchronized void recordFailure(String staffId) {
        Counter record = records.computeIfAbsent(key(staffId), k -> new Counter());
        clearIfExpired(record);
        record.failures++;
        if (record.failures >= MAX_FAILURES) {
            record.lockedUntil = clock.instant().plus(LOCK_TIME);
        }
    }

    /** A correct login: start counting from zero again. */
    public synchronized void recordSuccess(String staffId) {
        records.remove(key(staffId));
    }

    public synchronized boolean isLocked(String staffId) {
        Counter record = records.get(key(staffId));
        if (record == null) {
            return false;
        }
        clearIfExpired(record);
        return record.lockedUntil != null;
    }

    /** @throws AccountLockedException if this Staff ID is locked right now */
    public synchronized void requireUnlocked(String staffId) {
        Counter record = records.get(key(staffId));
        if (record == null) {
            return;
        }
        clearIfExpired(record);
        if (record.lockedUntil != null) {
            throw new AccountLockedException(key(staffId), Duration.between(clock.instant(), record.lockedUntil));
        }
    }

    /** When the lock time is over, forget the old failures. */
    private void clearIfExpired(Counter record) {
        if (record.lockedUntil != null && !clock.instant().isBefore(record.lockedUntil)) {
            record.lockedUntil = null;
            record.failures = 0;
        }
    }

    private static String key(String staffId) {
        return staffId == null ? "" : staffId.strip().toUpperCase(Locale.ROOT);
    }
}
