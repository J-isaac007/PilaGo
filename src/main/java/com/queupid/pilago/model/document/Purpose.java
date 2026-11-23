package com.queupid.pilago.model.document;

/** Why the resident needs the document. Some fees depend on it. */
public enum Purpose {

    EMPLOYMENT("purpose.employment"),
    SCHOOL("purpose.school"),
    MEDICAL("purpose.medical"),
    FINANCIAL_ASSISTANCE("purpose.financial-assistance"),
    GOVERNMENT_ID("purpose.government-id"),
    OTHER("purpose.other");

    private final String messageKey;

    Purpose(String messageKey) {
        this.messageKey = messageKey;
    }

    /** Key in messages.properties for the English text shown on the page. */
    public String getMessageKey() {
        return messageKey;
    }
}
