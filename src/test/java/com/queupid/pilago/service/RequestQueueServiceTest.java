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
import com.queupid.pilago.storage.InMemoryRequestStorage;

class RequestQueueServiceTest {

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
    private final InMemoryRequestStorage storage = new InMemoryRequestStorage();
    private final RequestQueueService queue = new RequestQueueService(clock, storage);

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
        assertThat(new SampleDay(queue).load()).isTrue();
        assertThat(queue.getNowServing()).get().extracting(Request::getTicketNumber).isEqualTo("C-003");
        assertThat(queue.getWaiting()).extracting(Request::getTicketNumber)
                .containsExactly("P-005", "S-008", "C-006", "C-009");
        assertThat(queue.getForSignature()).extracting(Request::getTicketNumber).containsExactly("S-004");
        assertThat(queue.getReady()).extracting(Request::getTicketNumber).containsExactly("S-002");
        assertThat(queue.getOnHold()).extracting(Request::getTicketNumber).containsExactly("C-007");
        assertThat(queue.getDailySummary().getFeesCollected()).isEqualByComparingTo("100.00");
        assertThat(new SampleDay(queue).load()).as("only fills an empty day").isFalse();
    }

    @Test
    void restartReloadsTheDayFromStorage() {
        new SampleDay(queue).load();
        queue.verify("C-007"); // back in line between C-006 and C-009 (by ticket number)

        // a "restart": a brand-new queue on the same storage
        RequestQueueService restarted = new RequestQueueService(clock, storage);
        assertThat(restarted.getNowServing()).get().extracting(Request::getTicketNumber).isEqualTo("C-003");
        assertThat(restarted.getWaiting()).extracting(Request::getTicketNumber)
                .containsExactly("P-005", "S-008", "C-006", "C-007", "C-009");
        assertThat(restarted.getForSignature()).extracting(Request::getTicketNumber).containsExactly("S-004");
        assertThat(restarted.getDailySummary().getTotalRequests()).isEqualTo(9);
        assertThatThrownBy(restarted::callNext).as("C-003 is still at the counter").isInstanceOf(QueueBusyException.class);

        // numbering continues after the restart: the next ticket is number 10
        Request next = restarted.addRequest(new RequestForm(DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.MEDICAL,
                "Lim, Rosa", 70, "Alangilan", 5, 0, false, false));
        assertThat(next.getTicketNumber()).isEqualTo("S-010");
        assertThat(storage.lastSequence(next.getTicketDate())).isEqualTo(10);
    }

    @Test
    void everyChangeIsSaved() {
        add("A", 30, false);
        queue.callNext();
        RequestQueueService restarted = new RequestQueueService(clock, storage);
        assertThat(restarted.findByTicket("C-001")).get().extracting(Request::getStatus)
                .isEqualTo(RequestStatus.PROCESSING);
    }

    @Test
    void afterMidnightTheNewDayStartsEmptyAtOne() {
        new SampleDay(queue).load();
        clock.plusDays(1);
        assertThat(queue.getAllToday()).isEmpty();
        assertThat(queue.getNowServing()).isEmpty();
        assertThat(add("A", 30, false).getTicketNumber()).isEqualTo("C-001");
    }
}
