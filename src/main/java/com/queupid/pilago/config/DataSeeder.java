package com.queupid.pilago.config;

import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.queupid.pilago.model.person.BarangayCaptain;
import com.queupid.pilago.model.person.BarangaySecretary;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.storage.StaffRepository;

/**
 * Makes sure one secretary and one captain account exist, with the passwords from the properties: fixed
 * dev values locally, Render environment variables in production. The first start creates the accounts.
 * On later starts, if the password property was changed (for example a new {@code SECRETARY_PASSWORD} on
 * Render), the account's password is changed to match, so the old one stops working.
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
    private final String secretaryName;
    private final String captainName;

    public DataSeeder(StaffRepository staff, PasswordEncoder encoder,
            @Value("${pilago.seed.secretary-id}") String secretaryId,
            @Value("${pilago.seed.secretary-password}") String secretaryPassword,
            @Value("${pilago.seed.captain-id}") String captainId,
            @Value("${pilago.seed.captain-password}") String captainPassword,
            @Value("${pilago.seed.secretary-name:Maria Santos}") String secretaryName,
            @Value("${pilago.seed.captain-name:Hon. Roberto Mendoza}") String captainName) {
        this.staff = staff;
        this.encoder = encoder;
        this.secretaryId = secretaryId;
        this.secretaryPassword = secretaryPassword;
        this.captainId = captainId;
        this.captainPassword = captainPassword;
        this.secretaryName = secretaryName;
        this.captainName = captainName;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureAccount("secretary", secretaryId, secretaryPassword,
                hash -> new BarangaySecretary(secretaryName, 35, "Barangay Hall", secretaryId, hash));
        ensureAccount("captain", captainId, captainPassword,
                hash -> new BarangayCaptain(captainName, 50, "Barangay Hall", captainId, hash));
    }

    /** Creates the account, or changes its password if the property no longer matches it. */
    private void ensureAccount(String role, String staffId, String password, Function<String, StaffMember> create) {
        staff.findByStaffIdIgnoreCase(staffId).ifPresentOrElse(member -> {
            if (!encoder.matches(password, member.getPasswordHash())) {
                member.changePasswordHash(encoder.encode(password));
                staff.save(member);
                log.info("Changed the {} account {} password to the configured one", role, staffId);
            }
        }, () -> {
            staff.save(create.apply(encoder.encode(password)));
            log.info("Created {} account {}", role, staffId);
        });
    }
}
