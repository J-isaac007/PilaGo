package com.queupid.pilago.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.queupid.pilago.exception.DuplicateRequestException;
import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.exception.QueueBusyException;
import com.queupid.pilago.exception.TicketNotFoundException;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;

/**
 * THE CONTRACT between Part 1 (queue engine) and Parts 2 and 3 (pages).
 *
 * <p>Controllers depend on this interface only, never on an implementation. Until the real queue
 * lands, Spring injects {@link FakeRequestQueue}. Don't rename or remove methods without telling
 * the team; adding methods is fine.
 *
 * <p>Ticket arguments are today's ticket numbers, e.g. {@code "S-004"}. Every method that takes one
 * throws {@link TicketNotFoundException} if no such ticket exists today, and
 * {@link InvalidStatusTransitionException} if the ticket is not in a status where that action is allowed.
 */
public interface RequestQueue {

    // ── Resident (Part 2) ────────────────────────────────────────────────

    /**
     * Issues a ticket. Seniors and PWD go ahead of regular tickets.
     * If residency fails (under 6 months, or a pending record) the ticket is still issued but starts
     * ON_HOLD, with {@link Request#getHoldReason()} set.
     *
     * @throws DuplicateRequestException if the same person already has an active ticket today for the
     *         same document; {@link DuplicateRequestException#getExisting()} is that ticket
     */
    Request addRequest(RequestForm form);

    /** The resident's own ticket, from the link {@code /ticket/{token}}. */
    Optional<Request> findByToken(UUID token);

    /**
     * How many people are ahead of this ticket in the waiting line: 0 means "you're next".
     * Returns -1 when the ticket is not waiting (on hold, being served, done).
     */
    int getPositionInLine(String ticket);

    /** Rough wait in minutes for a waiting ticket, from the documents ahead of it; 0 if not waiting. */
    int getEstimatedWaitMinutes(String ticket);

    // ── Secretary (Part 3) ───────────────────────────────────────────────

    /**
     * Calls the next ticket (WAITING → PROCESSING) and makes it Now serving.
     * There is one counter, so the current Now serving ticket must be finished first
     * (forwarded, put on hold or skipped).
     *
     * @return the called ticket, or empty if nobody is waiting
     * @throws QueueBusyException if Now serving is still being processed
     */
    Optional<Request> callNext();

    /** The resident did not come to the counter: → NO_SHOW. Clears Now serving if it was this ticket. */
    void skip(String ticket);

    /** Records payment of the full fee. Free documents need no payment. */
    void markPaid(String ticket);

    /** PROCESSING → FOR_SIGNATURE. Fails if a fee is due and not paid. */
    void forwardToCaptain(String ticket);

    /** WAITING or PROCESSING → ON_HOLD with a reason; {@code note} may be null. */
    void putOnHold(String ticket, HoldReason reason, String note);

    /** Secretary checked an on-hold ticket and it is fine: ON_HOLD → WAITING, keeping its place. */
    void verify(String ticket);

    /** READY → RELEASED: the resident picked up the document. */
    void release(String ticket);

    // ── Captain (Part 3) ─────────────────────────────────────────────────

    /** FOR_SIGNATURE → READY. */
    void sign(String ticket);

    // ── Lists and lookups (everyone) ─────────────────────────────────────

    /** Any of today's tickets by number, e.g. for {@code /staff/request/S-004}. */
    Optional<Request> findByTicket(String ticket);

    /** The ticket most recently called, while it is still PROCESSING. */
    Optional<Request> getNowServing();

    /** WAITING tickets in serving order (priority first, then by ticket time). */
    List<Request> getWaiting();

    /** ON_HOLD tickets, oldest first. */
    List<Request> getOnHold();

    /** FOR_SIGNATURE tickets, oldest first. */
    List<Request> getForSignature();

    /** READY tickets, oldest first. */
    List<Request> getReady();

    /** Today's counts and fees. */
    DailySummary getDailySummary();
}
