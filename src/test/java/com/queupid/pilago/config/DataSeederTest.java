package com.queupid.pilago.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.storage.StaffRepository;

/**
 * The staff accounts follow the password settings: when the configured password changes (a new
 * {@code SECRETARY_PASSWORD} on Render), the next start changes the account to match.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pilago-seeder-test",
        "pilago.sample-data=false" })
class DataSeederTest {

    @Autowired
    DataSeeder seeder;

    @Autowired
    StaffRepository staff;

    @Autowired
    PasswordEncoder encoder;

    private StaffMember secretary() {
        return staff.findByStaffIdIgnoreCase("SEC-001").orElseThrow();
    }

    @Test
    void startupCreatesBothAccountsOnce() {
        seeder.run(null); // a second start
        assertThat(staff.findAll()).extracting(StaffMember::getStaffId).containsExactlyInAnyOrder("SEC-001", "CAP-001");
        assertThat(encoder.matches("secretary123", secretary().getPasswordHash())).isTrue();
    }

    @Test
    void aChangedPasswordSettingReplacesTheOldPassword() {
        // The account still has an old password from before the setting changed
        StaffMember member = secretary();
        member.changePasswordHash(encoder.encode("old-password"));
        staff.save(member);
        String captainHashBefore = staff.findByStaffIdIgnoreCase("CAP-001").orElseThrow().getPasswordHash();

        seeder.run(null);

        assertThat(encoder.matches("secretary123", secretary().getPasswordHash())).isTrue();
        assertThat(encoder.matches("old-password", secretary().getPasswordHash())).isFalse();
        // an account whose password already matches is left alone
        assertThat(staff.findByStaffIdIgnoreCase("CAP-001").orElseThrow().getPasswordHash()).isEqualTo(captainHashBefore);
    }
}
