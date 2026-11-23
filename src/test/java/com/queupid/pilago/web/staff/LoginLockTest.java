package com.queupid.pilago.web.staff;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.hamcrest.Matchers.containsString;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.queupid.pilago.security.MutableClock;

/** 5 wrong passwords lock a Staff ID for 10 minutes; the lock ends by itself. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-lock-test")
@AutoConfigureMockMvc
@Import(LoginLockTest.MovableClock.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LoginLockTest {

    @TestConfiguration
    static class MovableClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    Clock clock;

    private void login(String id, String password, String expectedRedirect) throws Exception {
        mvc.perform(post("/login").with(csrf()).param("staffId", id).param("password", password))
                .andExpect(redirectedUrl(expectedRedirect));
    }

    @Test
    void fifthWrongPasswordLocksAndTheRightPasswordIsRefused() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login("SEC-001", "wrong", "/login?error");
        }
        login("SEC-001", "wrong", "/login?locked");          // 5th wrong try
        login("SEC-001", "secretary123", "/login?locked");   // even the right password is refused
        login("CAP-001", "captain123", "/captain");          // other Staff IDs are not affected
    }

    @Test
    void lockEndsAfterTenMinutes() throws Exception {
        for (int i = 1; i <= 5; i++) {
            login("SEC-001", "wrong", i < 5 ? "/login?error" : "/login?locked");
        }
        ((MutableClock) clock).plus(Duration.ofMinutes(9));
        login("SEC-001", "secretary123", "/login?locked");
        ((MutableClock) clock).plus(Duration.ofMinutes(1));
        login("SEC-001", "secretary123", "/staff");
    }

    @Test
    void lockedPageShowsTheBilingualMessage() throws Exception {
        mvc.perform(get("/login").param("locked", ""))
                .andExpect(content().string(containsString(
                        "Too many tries. Try again in 10 minutes")));
    }
}
