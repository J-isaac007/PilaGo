package com.queupid.pilago.exception;

/** "Call next" was pressed while the ticket at the counter is still being processed. */
public class QueueBusyException extends PilaGoException {

    private final String nowServing;

    public QueueBusyException(String nowServing) {
        super("Finish " + nowServing + " first: forward it, put it on hold, or skip it", "error.queue-busy");
        this.nowServing = nowServing;
    }

    public String getNowServing() {
        return nowServing;
    }
}
