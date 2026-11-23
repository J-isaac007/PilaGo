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
 * {@link #LOCK_TIME}. Wrong tries more than {@link #LOCK_TIME} apart don't add up, and old counters are
 * forgotten, so made-up Staff IDs don't pile up in memory. Encapsulation: the counters are private, and
 * the only way to change them is {@link #recordFailure}, {@link #recordSuccess} and time passing. It reads
 * time from the app's {@link Clock}, so tests can move time forward.
 *
 * <p>Trade-off: the login page is public, so anyone who knows a Staff ID can lock it for 10 minutes by
 * typing wrong passwords. That is the price of stopping password guessing; the lock ends by itself.
 */
@Component
public class LoginAttempts {

    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(10);

    /** One Staff ID's counter. */
    private static final class Counter {
        private int failures;
        private Instant lastFailure;
        private Instant lockedUntil;

        /** Nothing left to remember: the lock is over, or the last wrong try was long ago. */
        private boolean isOver(Instant now) {
            Instant end = lockedUntil != null ? lockedUntil : lastFailure.plus(LOCK_TIME);
            return !now.isBefore(end);
        }
    }

    private final Clock clock;
    private final Map<String, Counter> records = new HashMap<>();

    public LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** A wrong password. The 5th one in a row starts the lock. */
    public synchronized void recordFailure(String staffId) {
        Instant now = clock.instant();
        records.values().removeIf(record -> record.isOver(now));
        Counter record = records.computeIfAbsent(key(staffId), k -> new Counter());
        record.failures++;
        record.lastFailure = now;
        if (record.failures >= MAX_FAILURES) {
            record.lockedUntil = now.plus(LOCK_TIME);
        }
    }

    /** A correct login: start counting from zero again. */
    public synchronized void recordSuccess(String staffId) {
        records.remove(key(staffId));
    }

    public synchronized boolean isLocked(String staffId) {
        return current(staffId) != null;
    }

    /** @throws AccountLockedException if this Staff ID is locked right now */
    public synchronized void requireUnlocked(String staffId) {
        Counter record = current(staffId);
        if (record != null) {
            throw new AccountLockedException(key(staffId), Duration.between(clock.instant(), record.lockedUntil));
        }
    }

    /** How many Staff IDs are being counted right now (for tests). */
    synchronized int trackedIds() {
        Instant now = clock.instant();
        records.values().removeIf(record -> record.isOver(now));
        return records.size();
    }

    /** This Staff ID's counter if it is locked right now, else null (and an old counter is forgotten). */
    private Counter current(String staffId) {
        Counter record = records.get(key(staffId));
        if (record == null) {
            return null;
        }
        if (record.isOver(clock.instant())) {
            records.remove(key(staffId));
            return null;
        }
        return record.lockedUntil != null ? record : null;
    }

    private static String key(String staffId) {
        return staffId == null ? "" : staffId.strip().toUpperCase(Locale.ROOT);
    }
}
