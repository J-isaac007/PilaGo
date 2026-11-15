package com.queupid.pilago.model.request;

import java.util.EnumSet;
import java.util.Set;

/**
 * Where a request is in its life. {@link #canTransitionTo(RequestStatus)} lists the only moves
 * allowed, so a request can never jump to an invalid state.
 *
 * <pre>
 * WAITING ──call──▶ PROCESSING ──forward──▶ FOR_SIGNATURE ──sign──▶ READY ──release──▶ RELEASED
 *    │  ▲               │  │
 *    │  └──verify── ON_HOLD ◀──hold──┘  └──skip──▶ NO_SHOW
 *    └──────hold─────────┘
 * </pre>
 */
public enum RequestStatus {

    WAITING("status.waiting"),
    ON_HOLD("status.on-hold"),
    PROCESSING("status.processing"),
    FOR_SIGNATURE("status.for-signature"),
    READY("status.ready"),
    RELEASED("status.released"),
    NO_SHOW("status.skipped");

    private final String messageKey;

    RequestStatus(String messageKey) {
        this.messageKey = messageKey;
    }

    public boolean canTransitionTo(RequestStatus next) {
        return allowedNext().contains(next);
    }

    /** The statuses this one may move to. */
    public Set<RequestStatus> allowedNext() {
        return switch (this) {
            case WAITING -> EnumSet.of(PROCESSING, ON_HOLD, NO_SHOW);
            case ON_HOLD -> EnumSet.of(WAITING, PROCESSING);
            case PROCESSING -> EnumSet.of(FOR_SIGNATURE, ON_HOLD, NO_SHOW);
            case FOR_SIGNATURE -> EnumSet.of(READY);
            case READY -> EnumSet.of(RELEASED);
            case RELEASED, NO_SHOW -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    /** Still in the system today (not released and not skipped). */
    public boolean isActive() {
        return this != RELEASED && this != NO_SHOW;
    }

    /** Key in messages.properties; add {@code .fil} for the Filipino text. */
    public String getMessageKey() {
        return messageKey;
    }

    /** CSS modifier for the status badge, e.g. {@code pg-badge--on-hold}, {@code pg-badge--no-show}. */
    public String getCssModifier() {
        return name().toLowerCase().replace('_', '-');
    }
}
