package com.queupid.pilago.exception;

/** The captain picked "Other" when returning a request but did not say what to fix. */
public class ReturnNoteRequiredException extends PilaGoException {

    public ReturnNoteRequiredException(String ticketNumber) {
        super(ticketNumber + ": a note is required when the reason is Other", "error.return-note-required");
    }
}
