package com.queupid.pilago.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.queupid.pilago.config.TimeConfig;
import com.queupid.pilago.exception.DuplicateRequestException;
import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.exception.QueueBusyException;
import com.queupid.pilago.exception.TicketNotFoundException;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;

class FakeRequestQueueTest {

    /** A clock the test can move forward. */
    static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-11-16T01:00:00Z"); // 9:00 AM in Manila

        void plusDays(long days) {
            now = now.plus(days, ChronoUnit.DAYS);
        }

        @Override
        public ZoneId getZone() {
            return TimeConfig.MANILA;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final TestClock clock = new TestClock();
    private final FakeRequestQueue queue = new FakeRequestQueue(clock, false);

    private Request add(String name, int age, boolean pwd) {
        return queue.addRequest(new RequestForm(DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.SCHOOL,
                name, age, "Alangilan", 2, 0, pwd, false));
    }

    @Test
    void seniorsAndPwdGoAheadOfRegular() {
        add("Regular One", 30, false);   // C-001
        add("Senior", 70, false);        // S-002
        add("Regular Two", 30, false);   // C-003
        add("Pwd", 40, true);            // P-004
        assertThat(queue.getWaiting()).extracting(Request::getTicketNumber)
                .containsExactly("S-002", "P-004", "C-001", "C-003");
        assertThat(queue.getPositionInLine("C-001")).isEqualTo(2);
    }

    @Test
    void shortResidencyStartsOnHoldAndVerifyKeepsItsPlace() {
        add("A", 30, false); // C-001
        Request newcomer = queue.addRequest(new RequestForm(DocumentType.BARANGAY_CLEARANCE, Purpose.OTHER,
                "Newcomer", 30, "Alangilan", 0, 3, false, false)); // C-002
        add("C", 30, false); // C-003
        assertThat(newcomer.getStatus()).isEqualTo(RequestStatus.ON_HOLD);
        assertThat(newcomer.getHoldReason()).isEqualTo(HoldReason.SHORT_RESIDENCY);
        assertThat(queue.getPositionInLine("C-002")).isEqualTo(-1);

        queue.verify("C-002");
        assertThat(queue.getWaiting()).extracting(Request::getTicketNumber).containsExactly("C-001", "C-002", "C-003");
    }

    @Test
    void duplicateTicketIsRefused() {
        Request first = add("Juan Dela Cruz", 30, false);
        assertThatThrownBy(() -> add("juan dela cruz", 30, false))
                .isInstanceOf(DuplicateRequestException.class)
                .satisfies(e -> assertThat(((DuplicateRequestException) e).getExisting()).isSameAs(first));
    }

    @Test
    void fullFlowAndDailySummary() {
        Request req = queue.addRequest(new RequestForm(DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT,
                "Ana", 23, "Alangilan", 5, 0, false, false));
        assertThat(queue.callNext()).contains(req);
        assertThat(queue.getNowServing()).contains(req);
        assertThatThrownBy(() -> queue.forwardToCaptain("C-001")).isInstanceOf(InvalidStatusTransitionException.class);
        queue.markPaid("c-001");
        queue.forwardToCaptain("C-001");
        assertThat(queue.getNowServing()).isEmpty();
        assertThat(queue.getForSignature()).containsExactly(req);
        queue.sign("C-001");
        queue.release("C-001");

        DailySummary summary = queue.getDailySummary();
        assertThat(summary.getTotalRequests()).isEqualTo(1);
        assertThat(summary.getReleased()).isEqualTo(1);
        assertThat(summary.getFeesCollected()).isEqualByComparingTo("50.00");
    }

    @Test
    void cannotCallNextWhileServingAndSkipFreesTheCounter() {
        add("A", 30, false);
        add("B", 30, false);
        queue.callNext();
        assertThatThrownBy(queue::callNext).isInstanceOf(QueueBusyException.class);
        queue.skip("C-001");
        assertThat(queue.findByTicket("C-001")).get().extracting(Request::getStatus).isEqualTo(RequestStatus.NO_SHOW);
        assertThat(queue.callNext()).get().extracting(Request::getTicketNumber).isEqualTo("C-002");
    }

    @Test
    void emptyQueueAndUnknownTickets() {
        assertThat(queue.callNext()).isEmpty();
        assertThatThrownBy(() -> queue.sign("Z-999")).isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void numbersRestartTheNextDay() {
        add("A", 30, false);
        clock.plusDays(1);
        assertThat(queue.getWaiting()).isEmpty();
        assertThat(add("A", 30, false).getTicketNumber()).isEqualTo("C-001");
    }

    @Test
    void sampleDayMatchesFigma() {
        FakeRequestQueue sample = new FakeRequestQueue(clock);
        assertThat(sample.getNowServing()).get().extracting(Request::getTicketNumber).isEqualTo("C-003");
        assertThat(sample.getWaiting()).extracting(Request::getTicketNumber)
                .containsExactly("P-005", "S-008", "C-006", "C-009");
        assertThat(sample.getForSignature()).extracting(Request::getTicketNumber).containsExactly("S-004");
        assertThat(sample.getReady()).extracting(Request::getTicketNumber).containsExactly("S-002");
        assertThat(sample.getOnHold()).extracting(Request::getTicketNumber).containsExactly("C-007");
        assertThat(sample.getDailySummary().getFeesCollected()).isEqualByComparingTo("100.00");
    }
}
