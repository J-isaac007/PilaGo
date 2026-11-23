package com.queupid.pilago;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.queupid.pilago.service.RequestQueue;
import com.queupid.pilago.storage.StaffRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:pilago-test")
class PilaGoSystemTests {

    @Autowired
    RequestQueue queue;

    @Autowired
    StaffRepository staff;

    @Test
    void appStartsWithQueueAndStaffAccounts() {
        assertThat(queue.getNowServing()).isPresent();
        assertThat(staff.findByStaffIdIgnoreCase("sec-001")).get()
                .extracting(s -> s.getRole()).isEqualTo("SECRETARY");
        assertThat(staff.findByStaffIdIgnoreCase("CAP-001")).isPresent();
    }

    /** The app is English-only (matching Figma): no Filipino ".fil" keys, and no empty texts. */
    @ParameterizedTest
    @ValueSource(strings = {"messages", "messages_resident", "messages_staff"})
    void everyMessageIsEnglishOnlyAndFilledIn(String bundle) throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/" + bundle + ".properties")) {
            assertThat(in).as(bundle).isNotNull();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        List<String> problems = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (key.endsWith(".fil")) {
                problems.add(key + " (Filipino key)");
            } else if (props.getProperty(key).isBlank()) {
                problems.add(key + " (empty)");
            }
        }
        assertThat(problems).as("problems in " + bundle).isEmpty();
    }
}
