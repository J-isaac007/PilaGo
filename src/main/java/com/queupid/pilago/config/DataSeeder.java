package com.queupid.pilago.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.queupid.pilago.model.person.BarangayCaptain;
import com.queupid.pilago.model.person.BarangaySecretary;
import com.queupid.pilago.storage.StaffRepository;

/**
 * Creates one secretary and one captain account the first time the app starts. Passwords come from
 * properties: fixed dev values locally, Render environment variables in production.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final StaffRepository staff;
    private final PasswordEncoder encoder;
    private final String secretaryId;
    private final String secretaryPassword;
    private final String captainId;
    private final String captainPassword;

    public DataSeeder(StaffRepository staff, PasswordEncoder encoder,
            @Value("${pilago.seed.secretary-id}") String secretaryId,
            @Value("${pilago.seed.secretary-password}") String secretaryPassword,
            @Value("${pilago.seed.captain-id}") String captainId,
            @Value("${pilago.seed.captain-password}") String captainPassword) {
        this.staff = staff;
        this.encoder = encoder;
        this.secretaryId = secretaryId;
        this.secretaryPassword = secretaryPassword;
        this.captainId = captainId;
        this.captainPassword = captainPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (staff.findByStaffIdIgnoreCase(secretaryId).isEmpty()) {
            staff.save(new BarangaySecretary("Brgy. Secretary", 35, "Barangay Hall", secretaryId,
                    encoder.encode(secretaryPassword)));
            log.info("Created secretary account {}", secretaryId);
        }
        if (staff.findByStaffIdIgnoreCase(captainId).isEmpty()) {
            staff.save(new BarangayCaptain("Brgy. Captain", 50, "Barangay Hall", captainId,
                    encoder.encode(captainPassword)));
            log.info("Created captain account {}", captainId);
        }
    }
}
