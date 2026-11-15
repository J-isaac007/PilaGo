package com.queupid.pilago.exception;

import com.queupid.pilago.model.request.RequestStatus;

/** A request was asked to move to a status it cannot reach from where it is. */
public class InvalidStatusTransitionException extends PilaGoException {

    private final String ticketNumber;
    private final RequestStatus from;
    private final RequestStatus to;

    public InvalidStatusTransitionException(String ticketNumber, RequestStatus from, RequestStatus to) {
        this(ticketNumber, from, to, null);
    }

    public InvalidStatusTransitionException(String ticketNumber, RequestStatus from, RequestStatus to, String why) {
        super("%s cannot go from %s to %s%s".formatted(ticketNumber, from, to, why == null ? "" : ": " + why),
                "error.invalid-transition");
        this.ticketNumber = ticketNumber;
        this.from = from;
        this.to = to;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public RequestStatus getFrom() {
        return from;
    }

    public RequestStatus getTo() {
        return to;
    }
}
