package com.queupid.pilago.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.queupid.pilago.exception.DuplicateRequestException;
import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.exception.QueueBusyException;
import com.queupid.pilago.exception.TicketNotFoundException;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.person.StaffMember;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;
import com.queupid.pilago.storage.RequestStorage;

/**
 * The queue engine. Today's tickets live in {@code ArrayList}s in memory (fast, and the order of the
 * waiting line is easy to see), and every change is written to {@link RequestStorage} right away, so
 * nothing is lost when the app restarts.
 *
 * <ul>
 *   <li><b>Priority line:</b> seniors and PWD go ahead of regular tickets; inside each tier it is first
 *       come, first served ({@link #insertInLine}).</li>
 *   <li><b>Ticket numbers:</b> one shared counter per day ({@code S-004}, {@code P-005}, {@code C-006}).
 *       After a restart it continues from the highest number saved today; at midnight it starts at 1.</li>
 *   <li><b>Reload:</b> the first call of a day loads that day's tickets from storage and rebuilds the
 *       waiting line and Now serving.</li>
 *   <li><b>Thread safety:</b> every public method is {@code synchronized}, so two staff browsers can't
 *       call the same ticket twice.</li>
 * </ul>
 */
@Service
public class RequestQueueService implements RequestQueue {

    private final Clock clock;
    private final RequestStorage storage;

    /** Every ticket issued today, in the order issued. */
    private final List<Request> today = new ArrayList<>();

    /** WAITING tickets in serving order: priority tier first, then ticket number. */
    private final List<Request> waitingRequests = new ArrayList<>();

    private Request nowServing;
    private LocalDate currentDay;
    private int lastSequence;

    public RequestQueueService(Clock clock, RequestStorage storage) {
        this.clock = clock;
        this.storage = storage;
    }

    // ── Resident ─────────────────────────────────────────────────────────

    @Override
    public synchronized Request addRequest(RequestForm form) {
        rollOverIfNewDay();
        Resident resident = new Resident(form.getFullName(), form.getAge(), form.getAddress(),
                form.getTotalResidencyMonths(), form.isPwd(), form.isHasPendingRecord());

        for (Request existing : today) {
            boolean samePerson = existing.getResident().getName().equalsIgnoreCase(resident.getName());
            if (samePerson && existing.getDocumentType() == form.getDocumentType() && existing.getStatus().isActive()) {
                throw new DuplicateRequestException(existing);
            }
        }

        Request request = new Request(resident, form.getDocumentType(), form.getPurpose(),
                currentDay, ++lastSequence, now());
        try {
            resident.checkResidency();
            insertInLine(request);
        } catch (NotAResidentException e) {
            request.putOnHold(e.getReason(), null, now());
        }
        storage.save(request);
        today.add(request);
        return request;
    }

    @Override
    public synchronized Optional<Request> findByToken(UUID token) {
        rollOverIfNewDay();
        return today.stream().filter(r -> r.getToken().equals(token)).findFirst();
    }

    @Override
    public synchronized int getPositionInLine(String ticket) {
        return waitingRequests.indexOf(require(ticket));
    }

    @Override
    public synchronized int getEstimatedWaitMinutes(String ticket) {
        int position = getPositionInLine(ticket);
        if (position < 0) {
            return 0;
        }
        int minutes = 0;
        for (int i = 0; i < position; i++) {
            minutes += waitingRequests.get(i).getDocument().getProcessingTime();
        }
        if (nowServing != null) {
            minutes += nowServing.getDocument().getProcessingTime() / 2;
        }
        return minutes;
    }

    // ── Secretary ────────────────────────────────────────────────────────

    @Override
    public synchronized Optional<Request> callNext(StaffMember by) {
        rollOverIfNewDay();
        if (nowServing != null && nowServing.getStatus() == RequestStatus.PROCESSING) {
            throw new QueueBusyException(nowServing.getTicketNumber());
        }
        if (waitingRequests.isEmpty()) {
            nowServing = null;
            return Optional.empty();
        }
        Request next = waitingRequests.get(0);
        next.call(by, now());
        waitingRequests.remove(0);
        nowServing = next;
        storage.save(next);
        return Optional.of(next);
    }

    @Override
    public synchronized void skip(String ticket) {
        Request request = require(ticket);
        request.updateStatus(RequestStatus.NO_SHOW, now());
        leaveLineAndCounter(request);
        storage.save(request);
    }

    @Override
    public synchronized void markPaid(String ticket, StaffMember by) {
        Request request = require(ticket);
        if (request.getStatus() != RequestStatus.PROCESSING) {
            throw new InvalidStatusTransitionException(ticket, request.getStatus(), RequestStatus.PROCESSING,
                    "payment is recorded while the ticket is at the counter");
        }
        request.markAsPaid(by, now());
        storage.save(request);
    }

    @Override
    public synchronized void forwardToCaptain(String ticket, StaffMember by) {
        Request request = require(ticket);
        request.forwardToCaptain(by, now());
        leaveLineAndCounter(request);
        storage.save(request);
    }

    @Override
    public synchronized void putOnHold(String ticket, HoldReason reason, String note) {
        Request request = require(ticket);
        if (request.getStatus() != RequestStatus.WAITING && request.getStatus() != RequestStatus.PROCESSING) {
            throw new InvalidStatusTransitionException(ticket, request.getStatus(), RequestStatus.ON_HOLD,
                    "the secretary can only hold tickets that are waiting or at the counter");
        }
        request.putOnHold(reason, note, now());
        leaveLineAndCounter(request);
        storage.save(request);
    }

    @Override
    public synchronized void verify(String ticket) {
        Request request = require(ticket);
        if (request.getStatus() != RequestStatus.ON_HOLD) {
            throw new InvalidStatusTransitionException(ticket, request.getStatus(), RequestStatus.WAITING,
                    "only tickets on hold can be verified");
        }
        request.updateStatus(RequestStatus.WAITING, now());
        insertInLine(request);
        storage.save(request);
    }

    @Override
    public synchronized void release(String ticket, StaffMember by) {
        Request request = require(ticket);
        request.release(by, now());
        storage.save(request);
    }

    // ── Captain ──────────────────────────────────────────────────────────

    @Override
    public synchronized void sign(String ticket, StaffMember by) {
        Request request = require(ticket);
        request.sign(by, now());
        storage.save(request);
    }

    @Override
    public synchronized void returnToSecretary(String ticket, HoldReason reason, String note, StaffMember by) {
        Request request = require(ticket);
        request.returnToSecretary(reason, note, by, now());
        storage.save(request);
    }

    // ── Lists and lookups ────────────────────────────────────────────────

    @Override
    public synchronized Optional<Request> findByTicket(String ticket) {
        rollOverIfNewDay();
        if (ticket == null) {
            return Optional.empty();
        }
        String wanted = ticket.strip().toUpperCase();
        return today.stream().filter(r -> r.getTicketNumber().equals(wanted)).findFirst();
    }

    @Override
    public synchronized Optional<Request> getNowServing() {
        rollOverIfNewDay();
        if (nowServing != null && nowServing.getStatus() == RequestStatus.PROCESSING) {
            return Optional.of(nowServing);
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<Request> getWaiting() {
        rollOverIfNewDay();
        return List.copyOf(waitingRequests);
    }

    @Override
    public synchronized List<Request> getOnHold() {
        return withStatus(RequestStatus.ON_HOLD);
    }

    @Override
    public synchronized List<Request> getForSignature() {
        return withStatus(RequestStatus.FOR_SIGNATURE);
    }

    @Override
    public synchronized List<Request> getReady() {
        return withStatus(RequestStatus.READY);
    }

    @Override
    public synchronized List<Request> getAllToday() {
        rollOverIfNewDay();
        return List.copyOf(today);
    }

    @Override
    public synchronized DailySummary getDailySummary() {
        rollOverIfNewDay();
        Map<RequestStatus, Integer> counts = new EnumMap<>(RequestStatus.class);
        Map<DocumentType, Integer> byDocument = new EnumMap<>(DocumentType.class);
        BigDecimal fees = BigDecimal.ZERO;
        for (Request r : today) {
            counts.merge(r.getStatus(), 1, Integer::sum);
            byDocument.merge(r.getDocumentType(), 1, Integer::sum);
            if (r.isPaid() && r.getAmountPaid() != null) {
                fees = fees.add(r.getAmountPaid());
            }
        }
        return new DailySummary(currentDay, today.size(),
                counts.getOrDefault(RequestStatus.WAITING, 0),
                counts.getOrDefault(RequestStatus.ON_HOLD, 0),
                counts.getOrDefault(RequestStatus.FOR_SIGNATURE, 0),
                counts.getOrDefault(RequestStatus.READY, 0),
                counts.getOrDefault(RequestStatus.RELEASED, 0),
                counts.getOrDefault(RequestStatus.NO_SHOW, 0),
                fees, byDocument);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Priority insert: a ticket goes after every ticket in a higher-or-equal tier that was issued
     * before it. Seniors and PWD share the top tier; regular tickets come after them.
     */
    private void insertInLine(Request request) {
        int index = 0;
        while (index < waitingRequests.size() && comesBefore(waitingRequests.get(index), request)) {
            index++;
        }
        waitingRequests.add(index, request);
    }

    private static boolean comesBefore(Request a, Request b) {
        if (a.isPriority() != b.isPriority()) {
            return a.isPriority();
        }
        return a.getDailySequence() < b.getDailySequence();
    }

    private void leaveLineAndCounter(Request request) {
        waitingRequests.remove(request);
        if (request == nowServing) {
            nowServing = null;
        }
    }

    private List<Request> withStatus(RequestStatus status) {
        rollOverIfNewDay();
        List<Request> result = new ArrayList<>();
        for (Request r : today) {
            if (r.getStatus() == status) {
                result.add(r);
            }
        }
        return result;
    }

    private Request require(String ticket) {
        return findByTicket(ticket).orElseThrow(() -> new TicketNotFoundException(ticket));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** The first call of a new day (including the first call after a restart) loads that day. */
    private void rollOverIfNewDay() {
        LocalDate date = LocalDate.now(clock);
        if (!date.equals(currentDay)) {
            load(date);
        }
    }

    /** Rebuilds today's lists from storage: the waiting line in priority order, and Now serving. */
    private void load(LocalDate date) {
        today.clear();
        waitingRequests.clear();
        nowServing = null;
        currentDay = date;
        for (Request r : storage.findByDate(date)) {
            today.add(r);
            if (r.getStatus() == RequestStatus.WAITING) {
                insertInLine(r);
            } else if (r.getStatus() == RequestStatus.PROCESSING
                    && (nowServing == null || isAfter(r.getCalledAt(), nowServing.getCalledAt()))) {
                nowServing = r;
            }
        }
        lastSequence = storage.lastSequence(date);
    }

    private static boolean isAfter(LocalDateTime a, LocalDateTime b) {
        return a != null && (b == null || a.isAfter(b));
    }
}
