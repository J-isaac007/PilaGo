package com.queupid.pilago.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;

/**
 * Fills an empty day with the same tickets the Figma screens show, so the pages have something to
 * show in development and demos. It only uses the public {@link RequestQueue} methods, so the sample
 * follows every queue rule and is saved like real tickets.
 *
 * <p>On when {@code pilago.sample-data=true} (the dev profile; off on Render unless
 * {@code PILAGO_SAMPLE_DATA=true}). It does nothing if today already has tickets.
 *
 * <p>Result: C-001 released, S-002 ready, S-004 for signature, C-003 now serving,
 * P-005 / S-008 / C-006 / C-009 waiting (priority first), C-007 on hold (lived here 4 months).
 */
@Component
@ConditionalOnProperty(name = "pilago.sample-data", havingValue = "true")
public class SampleDay implements ApplicationRunner {

    private static final String ADDRESS = "Brgy. Alangilan, Batangas City";

    private final RequestQueue queue;

    public SampleDay(RequestQueue queue) {
        this.queue = queue;
    }

    @Override
    public void run(ApplicationArguments args) {
        load();
    }

    /** Adds the sample tickets if today is empty. Returns true if it added them. */
    public boolean load() {
        if (!queue.getAllToday().isEmpty()) {
            return false;
        }
        // C-001: served, paid, signed and picked up
        add("Aquino, Pedro T.", 34, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 48);
        queue.callNext();
        queue.markPaid("C-001");
        queue.forwardToCaptain("C-001");
        queue.sign("C-001");
        queue.release("C-001");

        // S-002: signed, waiting to be picked up
        add("Torres, Felipe G.", 67, false, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.GOVERNMENT_ID, 240);
        queue.callNext();
        queue.forwardToCaptain("S-002");
        queue.sign("S-002");

        // C-003 and S-004 come in; the senior is called first, paid and sent to the captain
        add("Dela Cruz, Juan P.", 29, false, DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.SCHOOL, 120);
        add("Bautista, Remedios A.", 72, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 360);
        queue.callNext();
        queue.markPaid("S-004");
        queue.forwardToCaptain("S-004");

        // C-003 is at the counter now
        queue.callNext();

        // still in line (C-007 starts on hold: only 4 months in the barangay)
        add("Villanueva, Carlo D.", 41, true, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.MEDICAL, 96);
        add("Reyes, Ana M.", 23, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 60);
        add("Santos, Mark L.", 31, false, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.SCHOOL, 4);
        add("Mendoza, Lourdes C.", 64, false, DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.MEDICAL, 300);
        add("Garcia, Liza R.", 38, false, DocumentType.BARANGAY_CLEARANCE, Purpose.OTHER, 84);
        return true;
    }

    private void add(String name, int age, boolean pwd, DocumentType type, Purpose purpose, int residencyMonths) {
        queue.addRequest(new RequestForm(type, purpose, name, age, ADDRESS,
                residencyMonths / 12, residencyMonths % 12, pwd, false));
    }
}
