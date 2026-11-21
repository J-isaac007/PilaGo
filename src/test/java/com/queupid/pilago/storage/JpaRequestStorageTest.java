package com.queupid.pilago.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;

/**
 * Tickets really go into the database and come back in order. Runs the whole app on its own in-memory
 * H2 database (without the sample day), the same way the app saves tickets.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:pilago-storage-test",
        "pilago.sample-data=false" })
class JpaRequestStorageTest {

    /** A day far from today, so nothing else in this database uses it. */
    private static final LocalDate DAY = LocalDate.of(2030, 1, 15);
    private static final LocalDateTime NINE_AM = DAY.atTime(9, 0);

    @Autowired
    JpaRequestStorage storage;

    private Request ticket(String name, int age, int sequence, LocalDate day) {
        Resident resident = new Resident(name, age, "Purok 1", 24, false, false);
        return new Request(resident, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, day, sequence, NINE_AM);
    }

    @Test
    void savesAndFindsTodaysTicketsInOrder() {
        storage.save(ticket("Second", 30, 2, DAY));
        storage.save(ticket("First", 70, 1, DAY));
        storage.save(ticket("Yesterday", 30, 7, DAY.minusDays(1)));

        assertThat(storage.findByDate(DAY)).extracting(Request::getTicketNumber).containsExactly("S-001", "C-002");
        assertThat(storage.findByDate(DAY).get(0).getResident().getName()).isEqualTo("First");
        assertThat(storage.lastSequence(DAY)).isEqualTo(2);
        assertThat(storage.lastSequence(DAY.plusDays(1))).isZero();
    }

    @Test
    void changesAreSaved() {
        LocalDate otherDay = DAY.plusDays(10); // the tests share one database
        Request r = storage.save(ticket("Ana", 30, 1, otherDay));
        r.updateStatus(RequestStatus.PROCESSING, NINE_AM.plusMinutes(5));
        r.markAsPaid(NINE_AM.plusMinutes(6));
        storage.save(r);

        Request loaded = storage.findByDate(otherDay).get(0);
        assertThat(loaded.getStatus()).isEqualTo(RequestStatus.PROCESSING);
        assertThat(loaded.isPaid()).isTrue();
        assertThat(loaded.getAmountPaid()).isEqualByComparingTo("50.00");
        assertThat(loaded.getCalledAt()).isEqualTo(NINE_AM.plusMinutes(5));
    }
}
