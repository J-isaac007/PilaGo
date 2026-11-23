package com.queupid.pilago.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.storage.JpaRequestStorage;

/**
 * A database made before the captain's return reasons existed still accepts them after startup.
 * Runs the app on its own in-memory H2 database and puts back the old fixed list on hold_reason.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pilago-enum-test",
        "pilago.sample-data=false" })
class EnumColumnMigrationTest {

    private static final LocalDate DAY = LocalDate.of(2030, 2, 1);
    private static final LocalDateTime NINE_AM = DAY.atTime(9, 0);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EnumColumnMigration migration;

    @Autowired
    JpaRequestStorage storage;

    private String columnType(String column) {
        return jdbc.queryForObject("""
                select data_type from information_schema.columns
                where upper(table_name) = upper(?) and upper(column_name) = upper(?)
                """, String.class, EnumColumnMigration.TABLE, column);
    }

    private Request heldFor(HoldReason reason, int sequence) {
        Resident resident = new Resident("Ana " + sequence, 30, "Purok 1", 24, false, false);
        Request request = new Request(resident, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, DAY, sequence, NINE_AM);
        request.putOnHold(reason, "check", NINE_AM.plusMinutes(1));
        return request;
    }

    @Test
    void startupLeavesNoFixedListOnTheEnumColumns() {
        for (String column : EnumColumnMigration.ENUM_COLUMNS) {
            assertThat(columnType(column)).as(column).isNotEqualToIgnoringCase("ENUM");
        }
    }

    @Test
    void anOldHoldReasonColumnAcceptsTheNewReasonsAfterTheFix() {
        // The column as the first deploy made it, before WRONG_DETAILS and the other return reasons
        jdbc.execute("alter table document_request alter column hold_reason set data type "
                + "enum('SHORT_RESIDENCY','PENDING_RECORD','MISSING_REQUIREMENT','OTHER')");
        assertThat(columnType("hold_reason")).isEqualToIgnoringCase("ENUM");

        assertThatThrownBy(() -> storage.save(heldFor(HoldReason.WRONG_DETAILS, 1)))
                .isInstanceOf(RuntimeException.class);

        migration.run(null);
        migration.run(null); // safe to run again

        assertThat(columnType("hold_reason")).isNotEqualToIgnoringCase("ENUM");
        Request saved = storage.save(heldFor(HoldReason.WRONG_DETAILS, 2));
        assertThat(storage.findByDate(DAY))
                .filteredOn(r -> r.getId().equals(saved.getId()))
                .singleElement()
                .extracting(Request::getHoldReason)
                .isEqualTo(HoldReason.WRONG_DETAILS);
    }
}
