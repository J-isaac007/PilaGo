package com.queupid.pilago.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class LoginAttemptsTest {

    private final MutableClock clock = new MutableClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    private void fail(String id, int times) {
        for (int i = 0; i < times; i++) {
            attempts.recordFailure(id);
        }
    }

    @Test
    void fourWrongPasswordsDoNotLock() {
        fail("SEC-001", 4);
        assertThat(attempts.isLocked("SEC-001")).isFalse();
    }

    @Test
    void fifthWrongPasswordLocksAndThrowsCustomException() {
        fail("SEC-001", 5);
        assertThat(attempts.isLocked("SEC-001")).isTrue();
        assertThatThrownBy(() -> attempts.requireUnlocked("SEC-001")).isInstanceOf(AccountLockedException.class);
    }

    @Test
    void lockEndsAfterTenMinutes() {
        fail("SEC-001", 5);
        clock.plus(Duration.ofMinutes(9));
        assertThat(attempts.isLocked("SEC-001")).isTrue();
        clock.plus(Duration.ofMinutes(1));
        assertThat(attempts.isLocked("SEC-001")).isFalse();
        fail("SEC-001", 4); // the count started again from zero
        assertThat(attempts.isLocked("SEC-001")).isFalse();
    }

    @Test
    void successResetsTheCountAndIdsAreIndependent() {
        fail("SEC-001", 4);
        attempts.recordSuccess("SEC-001");
        fail("SEC-001", 4);
        assertThat(attempts.isLocked("SEC-001")).isFalse();

        fail("CAP-001", 5);
        assertThat(attempts.isLocked("cap-001")).isTrue(); // capital letters don't matter
        assertThat(attempts.isLocked("SEC-001")).isFalse();
    }
}
