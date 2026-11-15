package com.queupid.pilago.model.request;

/** Why a request is on hold. The resident sees the matching bilingual message on their ticket. */
public enum HoldReason {

    /** Lived in the barangay less than six months. Set automatically. */
    SHORT_RESIDENCY("hold.short-residency"),
    /** Resident declared a pending barangay record. Set automatically. */
    PENDING_RECORD("hold.pending-record"),
    /** Secretary: a requirement is missing (ID, proof of address). */
    MISSING_REQUIREMENT("hold.missing-requirement"),
    /** Secretary: any other reason, explained in the note. */
    OTHER("hold.other");

    private final String messageKey;

    HoldReason(String messageKey) {
        this.messageKey = messageKey;
    }

    /** Key in messages.properties; add {@code .fil} for the Filipino text. */
    public String getMessageKey() {
        return messageKey;
    }
}
