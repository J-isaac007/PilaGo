package com.queupid.pilago.exception;

/** No ticket with this number exists today. */
public class TicketNotFoundException extends PilaGoException {

    public TicketNotFoundException(String ticketNumber) {
        super("No ticket " + ticketNumber + " today", "error.ticket-not-found");
    }
}
