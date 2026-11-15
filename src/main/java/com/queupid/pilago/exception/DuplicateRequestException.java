package com.queupid.pilago.exception;

import com.queupid.pilago.model.request.Request;

/** A resident already has an active ticket today for the same document. */
public class DuplicateRequestException extends PilaGoException {

    private final transient Request existing;

    public DuplicateRequestException(Request existing) {
        super(existing.getResident().getName() + " already has " + existing.getTicketNumber()
                + " for " + existing.getDocumentType().getEnglishName(), "error.duplicate");
        this.existing = existing;
    }

    /** The ticket they already have, so the page can link to it. */
    public Request getExisting() {
        return existing;
    }
}
