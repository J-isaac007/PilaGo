package com.queupid.pilago.security;

import java.time.Duration;

/** A Staff ID is locked because of too many wrong passwords in a row. */
public class AccountLockedException extends RuntimeException {

    private final String staffId;
    private final Duration timeLeft;

    public AccountLockedException(String staffId, Duration timeLeft) {
        super("Staff ID %s is locked for %d more minute(s)".formatted(staffId, Math.max(1, timeLeft.toMinutes())));
        this.staffId = staffId;
        this.timeLeft = timeLeft;
    }

    public String getStaffId() {
        return staffId;
    }

    /** How long until the lock ends. */
    public Duration getTimeLeft() {
        return timeLeft;
    }
}
