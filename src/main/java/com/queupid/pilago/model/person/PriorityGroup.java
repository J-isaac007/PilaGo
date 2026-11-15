package com.queupid.pilago.model.person;

/**
 * Which line a ticket belongs to. The letter is the ticket prefix ({@code S-004}).
 * Seniors and PWD are served before regular tickets; inside each tier it is first come, first served.
 */
public enum PriorityGroup {

    SENIOR("S", true, "priority.senior"),
    PWD("P", true, "priority.pwd"),
    REGULAR("C", false, "priority.regular");

    private final String prefix;
    private final boolean priority;
    private final String messageKey;

    PriorityGroup(String prefix, boolean priority, String messageKey) {
        this.prefix = prefix;
        this.priority = priority;
        this.messageKey = messageKey;
    }

    /** Ticket letter: S, P or C. */
    public String getPrefix() {
        return prefix;
    }

    /** True for seniors and PWD, who go ahead of regular tickets. */
    public boolean isPriority() {
        return priority;
    }

    /** Key in messages.properties; add {@code .fil} for the Filipino text. */
    public String getMessageKey() {
        return messageKey;
    }

    /** CSS modifier for the priority tag, e.g. {@code pg-tag--senior}. */
    public String getCssModifier() {
        return name().toLowerCase();
    }

    /** Formats a ticket number, e.g. {@code SENIOR.ticketNumber(4)} is {@code S-004}. */
    public String ticketNumber(int dailySequence) {
        return "%s-%03d".formatted(prefix, dailySequence);
    }
}
