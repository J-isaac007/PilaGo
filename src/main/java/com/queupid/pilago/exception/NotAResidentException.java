package com.queupid.pilago.exception;

import com.queupid.pilago.model.request.HoldReason;

/**
 * Thrown by {@code Resident.checkResidency()} when the resident needs the secretary's verification.
 *
 * <p>This is a checked exception on purpose: the queue must catch it (try-catch) and put the
 * request on hold instead of turning the resident away.
 */
public class NotAResidentException extends Exception {

    private final HoldReason reason;

    public NotAResidentException(HoldReason reason) {
        super(switch (reason) {
            case SHORT_RESIDENCY -> "Lived in the barangay less than 6 months";
            case PENDING_RECORD -> "Has a pending barangay record";
            default -> "Needs verification";
        });
        this.reason = reason;
    }

    public HoldReason getReason() {
        return reason;
    }
}
