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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.queupid.pilago.exception.DuplicateRequestException;
import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.exception.QueueBusyException;
import com.queupid.pilago.exception.TicketNotFoundException;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.Resident;
import com.queupid.pilago.model.request.HoldReason;
import com.queupid.pilago.model.request.Request;
import com.queupid.pilago.model.request.RequestStatus;

/**
 * In-memory queue with sample tickets that match the Figma screens (Now serving C-003, S-004 for
 * signature, P-005 next in line…). It follows every rule of the contract, but nothing is saved:
 * a restart brings back the sample day.
 *
 * <p>Parts 2 and 3 build their pages against this until the real, database-backed queue replaces it
 * in week 4. Then this class stays for tests only.
 */
@Service
public class FakeRequestQueue implements RequestQueue {

    private final Clock clock;
    private final boolean withSampleData;

    /** Every ticket issued today, in the order issued. */
    private final List<Request> today = new ArrayList<>();

    /** WAITING tickets in serving order: priority tier first, then ticket number. */
    private final List<Request> waitingRequests = new ArrayList<>();

    private Request nowServing;
    private LocalDate currentDay;
    private int lastSequence;

    @Autowired
    public FakeRequestQueue(Clock clock) {
        this(clock, true);
    }

    /** {@code withSampleData = false} starts an empty day, for tests. */
    public FakeRequestQueue(Clock clock, boolean withSampleData) {
        this.clock = clock;
        this.withSampleData = withSampleData;
        startNewDay();
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
        today.add(request);

        try {
            resident.checkResidency();
            insertInLine(request);
        } catch (NotAResidentException e) {
            request.putOnHold(e.getReason(), null, now());
        }
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
    public synchronized Optional<Request> callNext() {
        rollOverIfNewDay();
        if (nowServing != null && nowServing.getStatus() == RequestStatus.PROCESSING) {
            throw new QueueBusyException(nowServing.getTicketNumber());
        }
        if (waitingRequests.isEmpty()) {
            nowServing = null;
            return Optional.empty();
        }
        Request next = waitingRequests.remove(0);
        next.updateStatus(RequestStatus.PROCESSING, now());
        nowServing = next;
        return Optional.of(next);
    }

    @Override
    public synchronized void skip(String ticket) {
        Request request = require(ticket);
        request.updateStatus(RequestStatus.NO_SHOW, now());
        leaveLineAndCounter(request);
    }

    @Override
    public synchronized void markPaid(String ticket) {
        Request request = require(ticket);
        if (request.getStatus() != RequestStatus.PROCESSING) {
            throw new InvalidStatusTransitionException(ticket, request.getStatus(), RequestStatus.PROCESSING,
                    "payment is recorded while the ticket is at the counter");
        }
        request.markAsPaid(now());
    }

    @Override
    public synchronized void forwardToCaptain(String ticket) {
        Request request = require(ticket);
        request.updateStatus(RequestStatus.FOR_SIGNATURE, now());
        leaveLineAndCounter(request);
    }

    @Override
    public synchronized void putOnHold(String ticket, HoldReason reason, String note) {
        Request request = require(ticket);
        request.putOnHold(reason, note, now());
        leaveLineAndCounter(request);
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
    }

    @Override
    public synchronized void release(String ticket) {
        require(ticket).updateStatus(RequestStatus.RELEASED, now());
    }

    // ── Captain ──────────────────────────────────────────────────────────

    @Override
    public synchronized void sign(String ticket) {
        require(ticket).updateStatus(RequestStatus.READY, now());
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

    /** Ticket numbers restart at 1 every day. */
    private void rollOverIfNewDay() {
        if (!LocalDate.now(clock).equals(currentDay)) {
            startNewDay();
        }
    }

    private void startNewDay() {
        today.clear();
        waitingRequests.clear();
        nowServing = null;
        lastSequence = 0;
        currentDay = LocalDate.now(clock);
        if (withSampleData) {
            loadSampleDay();
        }
    }

    /** The same tickets the Figma screens show. */
    private void loadSampleDay() {
        Request c001 = sample("Aquino, Pedro T.", 34, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 48);
        Request s002 = sample("Torres, Felipe G.", 67, false, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.GOVERNMENT_ID, 240);
        Request c003 = sample("Dela Cruz, Juan P.", 29, false, DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.SCHOOL, 120);
        Request s004 = sample("Bautista, Remedios A.", 72, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 360);
        sample("Villanueva, Carlo D.", 41, true, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.MEDICAL, 96);
        sample("Reyes, Ana M.", 23, false, DocumentType.BARANGAY_CLEARANCE, Purpose.EMPLOYMENT, 60);
        sample("Santos, Mark L.", 31, false, DocumentType.CERTIFICATE_OF_RESIDENCY, Purpose.SCHOOL, 4);
        sample("Mendoza, Lourdes C.", 64, false, DocumentType.CERTIFICATE_OF_INDIGENCY, Purpose.MEDICAL, 300);
        sample("Garcia, Liza R.", 38, false, DocumentType.BARANGAY_CLEARANCE, Purpose.OTHER, 84);

        walk(c001, true, RequestStatus.FOR_SIGNATURE, RequestStatus.READY, RequestStatus.RELEASED);
        walk(s002, false, RequestStatus.FOR_SIGNATURE, RequestStatus.READY);
        walk(s004, true, RequestStatus.FOR_SIGNATURE);
        walk(c003, false);
        nowServing = c003;
    }

    private Request sample(String name, int age, boolean pwd, DocumentType type, Purpose purpose, int residencyMonths) {
        return addRequest(new RequestForm(type, purpose, name, age, "Brgy. Alangilan, Batangas City",
                residencyMonths / 12, residencyMonths % 12, pwd, false));
    }

    /** Moves a sample ticket from WAITING through the given statuses. */
    private void walk(Request request, boolean pay, RequestStatus... statuses) {
        waitingRequests.remove(request);
        request.updateStatus(RequestStatus.PROCESSING, now());
        if (pay) {
            request.markAsPaid(now());
        }
        for (RequestStatus status : statuses) {
            request.updateStatus(status, now());
        }
    }
}
