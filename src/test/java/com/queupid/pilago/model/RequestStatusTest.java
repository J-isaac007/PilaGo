package com.queupid.pilago.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;

class RequestStatusTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 9, 0);

    private static Request request(DocumentType type, Purpose purpose) {
        Resident r = new Resident("Ana Reyes", 23, "Alangilan", 60, false, false);
        return new Request(r, type, purpose, NOW.toLocalDate(), 6, NOW);
    }

    @Test
    void happyPathFromWaitingToReleased() {
        Request req = request(DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT);
        assertThat(req.getTicketNumber()).isEqualTo("C-006");
        req.updateStatus(RequestStatus.PROCESSING, NOW);
        req.markAsPaid(NOW);
        req.updateStatus(RequestStatus.FOR_SIGNATURE, NOW);
        req.updateStatus(RequestStatus.READY, NOW);
        req.updateStatus(RequestStatus.RELEASED, NOW);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.RELEASED);
        assertThat(req.getAmountPaid()).isEqualByComparingTo("50.00");
    }

    @Test
    void cannotSkipSteps() {
        Request req = request(DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.MEDICAL);
        assertThatThrownBy(() -> req.updateStatus(RequestStatus.READY, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(RequestStatus.RELEASED.allowedNext()).isEmpty();
        assertThat(RequestStatus.NO_SHOW.canTransitionTo(RequestStatus.WAITING)).isFalse();
    }

    @Test
    void cannotForwardUnpaidClearance() {
        Request req = request(DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT);
        req.updateStatus(RequestStatus.PROCESSING, NOW);
        assertThatThrownBy(() -> req.updateStatus(RequestStatus.FOR_SIGNATURE, NOW))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("not paid");
    }

    @Test
    void freeDocumentForwardsWithoutPayment() {
        Request req = request(DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.SCHOOL);
        req.updateStatus(RequestStatus.PROCESSING, NOW);
        req.updateStatus(RequestStatus.FOR_SIGNATURE, NOW);
        assertThat(req.getStatus()).isEqualTo(RequestStatus.FOR_SIGNATURE);
    }

    @Test
    void holdReasonIsClearedWhenVerified() {
        Request req = request(DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.SCHOOL);
        req.putOnHold(HoldReason.MISSING_REQUIREMENT, "  no ID  ", NOW);
        assertThat(req.getHoldReason()).isEqualTo(HoldReason.MISSING_REQUIREMENT);
        assertThat(req.getHoldNote()).isEqualTo("no ID");
        req.updateStatus(RequestStatus.WAITING, NOW);
        assertThat(req.getHoldReason()).isNull();
    }

    @Test
    void cssModifiersMatchTheThemeClasses() {
        assertThat(RequestStatus.ON_HOLD.getCssModifier()).isEqualTo("on-hold");
        assertThat(RequestStatus.NO_SHOW.getCssModifier()).isEqualTo("skipped");
    }
}
