package com.queupid.pilago.model.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import com.queupid.pilago.exception.InvalidStatusTransitionException;
import com.queupid.pilago.model.document.Document;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Payable;
import com.queupid.pilago.model.document.Purpose;
import com.queupid.pilago.model.person.PriorityGroup;
import com.queupid.pilago.model.person.Resident;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

/**
 * One ticket: links a resident to a document and tracks it from Waiting to Released.
 *
 * <p>Encapsulation: there is no {@code setStatus} or {@code setPaid}. The status changes only through
 * {@link #updateStatus}, which checks {@link RequestStatus#canTransitionTo}, and payment only
 * through {@link #markAsPaid}.
 */
@Entity
@Table(name = "document_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_per_day", columnNames = { "ticket_date", "ticket_number" }))
public class Request implements Payable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Secret part of the resident's ticket link {@code /ticket/{token}}. */
    @Column(nullable = false, unique = true)
    private UUID token;

    /** e.g. {@code S-004}. Unique per day only. */
    @Column(name = "ticket_number", nullable = false)
    private String ticketNumber;

    @Column(name = "ticket_date", nullable = false)
    private LocalDate ticketDate;

    /** The shared daily counter behind the ticket number (4 in S-004). */
    private int dailySequence;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    private Resident resident;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private boolean paid;
    private BigDecimal amountPaid;

    @Enumerated(EnumType.STRING)
    private HoldReason holdReason;
    private String holdNote;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime calledAt;

    @Transient
    private Document document;

    /** For JPA only. */
    protected Request() {
    }

    public Request(Resident resident, DocumentType documentType, Purpose purpose,
            LocalDate ticketDate, int dailySequence, LocalDateTime createdAt) {
        this.resident = Objects.requireNonNull(resident, "resident");
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.ticketDate = Objects.requireNonNull(ticketDate, "ticketDate");
        this.dailySequence = dailySequence;
        this.ticketNumber = resident.getPriorityGroup().ticketNumber(dailySequence);
        this.token = UUID.randomUUID();
        this.status = RequestStatus.WAITING;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /**
     * Moves the request to {@code next}.
     *
     * @throws InvalidStatusTransitionException if the move is not allowed, e.g. RELEASED → WAITING
     */
    public void updateStatus(RequestStatus next, LocalDateTime now) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidStatusTransitionException(ticketNumber, status, next);
        }
        if (next == RequestStatus.FOR_SIGNATURE && !paid && !isFree()) {
            throw new InvalidStatusTransitionException(ticketNumber, status, next, "fee not paid yet");
        }
        if (next == RequestStatus.PROCESSING) {
            calledAt = now;
        }
        if (status == RequestStatus.ON_HOLD) {
            holdReason = null;
            holdNote = null;
        }
        status = next;
        updatedAt = now;
    }

    /** Puts the request on hold and remembers why. */
    public void putOnHold(HoldReason reason, String note, LocalDateTime now) {
        updateStatus(RequestStatus.ON_HOLD, now);
        this.holdReason = Objects.requireNonNull(reason, "reason");
        this.holdNote = note == null || note.isBlank() ? null : note.strip();
    }

    /** Records that the resident paid the full fee at the counter. */
    public void markAsPaid(LocalDateTime now) {
        if (paid) {
            return;
        }
        this.paid = true;
        this.amountPaid = computeFee();
        this.updatedAt = now;
    }

    /** The document object, rebuilt from the stored type and purpose. */
    public Document getDocument() {
        if (document == null) {
            document = documentType.create(purpose);
        }
        return document;
    }

    @Override
    public BigDecimal computeFee() {
        return getDocument().computeFee();
    }

    /** Same as {@link #computeFee()}, for templates: {@code ${request.fee}}. */
    public BigDecimal getFee() {
        return computeFee();
    }

    public PriorityGroup getPriorityGroup() {
        return resident.getPriorityGroup();
    }

    public boolean isPriority() {
        return getPriorityGroup().isPriority();
    }

    public Long getId() {
        return id;
    }

    public UUID getToken() {
        return token;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public LocalDate getTicketDate() {
        return ticketDate;
    }

    public int getDailySequence() {
        return dailySequence;
    }

    public Resident getResident() {
        return resident;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public boolean isPaid() {
        return paid;
    }

    /** What was paid; {@code null} until {@link #markAsPaid} is called. */
    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    /** {@code null} unless the request is on hold. */
    public HoldReason getHoldReason() {
        return holdReason;
    }

    /** Secretary's note for the hold, or {@code null}. */
    public String getHoldNote() {
        return holdNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** When the secretary called this ticket; {@code null} if not yet called. */
    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    @Override
    public String toString() {
        return ticketNumber + " " + status + " " + documentType;
    }
}
