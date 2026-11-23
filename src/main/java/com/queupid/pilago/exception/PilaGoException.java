package com.queupid.pilago.exception;

/**
 * Parent of PilaGo's unchecked exceptions. Each one carries a message key so the
 * {@code GlobalExceptionHandler} can show a bilingual error.
 */
public abstract class PilaGoException extends RuntimeException {

    private final String messageKey;

    protected PilaGoException(String message, String messageKey) {
        super(message);
        this.messageKey = messageKey;
    }

    /** Key in messages.properties for the English text shown on the page. */
    public String getMessageKey() {
        return messageKey;
    }
}
