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

    /** Guards the bilingual rule: every English key has a Filipino twin, and the reverse. */
    @ParameterizedTest
    @ValueSource(strings = {"messages", "messages_resident", "messages_staff"})
    void everyMessageHasEnglishAndFilipino(String bundle) throws IOException {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/" + bundle + ".properties")) {
            assertThat(in).as(bundle).isNotNull();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        List<String> missing = new ArrayList<>();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("form.") || key.startsWith("typeMismatch")) {
                continue; // validation messages are one bilingual string
            }
            String twin = key.endsWith(".fil") ? key.substring(0, key.length() - 4) : key + ".fil";
            if (!props.containsKey(twin)) {
                missing.add(twin);
            }
        }
        assertThat(missing).as("missing keys in " + bundle).isEmpty();
    }
}
