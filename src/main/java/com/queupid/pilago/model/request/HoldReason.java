package com.queupid.pilago.model.request;

import java.util.List;

/**
 * Why a request is on hold. The resident sees the long message ({@link #getMessageKey()}) on their
 * ticket; staff screens can show the short label ({@link #getLabelKey()}).
 */
public enum HoldReason {

    /** Lived in the barangay less than six months. Set automatically. */
    SHORT_RESIDENCY("hold.short-residency"),
    /** Resident declared a pending barangay record. Set automatically. */
    PENDING_RECORD("hold.pending-record"),
    /** Secretary: a requirement is missing (ID, proof of address). */
    MISSING_REQUIREMENT("hold.missing-requirement"),
    /** Captain sent it back: the resident's details look wrong. */
    WRONG_DETAILS("hold.wrong-details"),
    /** Captain sent it back: the fee was not recorded as paid. */
    PAYMENT_NOT_CONFIRMED("hold.payment-not-confirmed"),
    /** Captain sent it back: the valid ID needs another look. */
    ID_NEEDS_CHECKING("hold.id-needs-checking"),
    /** Any other reason, explained in the note (the note is required when the captain picks it). */
    OTHER("hold.other");

    private final String messageKey;

    HoldReason(String messageKey) {
        this.messageKey = messageKey;
    }

    /** The reasons the captain can pick when returning a request (Figma S4B / S11b), in that order. */
    public static List<HoldReason> returnReasons() {
        return List.of(WRONG_DETAILS, PAYMENT_NOT_CONFIRMED, ID_NEEDS_CHECKING, OTHER);
    }

    /** The reasons the secretary can pick when putting a request on hold. */
    public static List<HoldReason> secretaryReasons() {
        return List.of(MISSING_REQUIREMENT, SHORT_RESIDENCY, PENDING_RECORD, OTHER);
    }

    /** Full sentence for the resident's ticket page. */
    public String getMessageKey() {
        return messageKey;
    }

    /** Short label for staff screens and the captain's reason buttons, e.g. "Wrong details". */
    public String getLabelKey() {
        return messageKey + ".label";
    }
}
