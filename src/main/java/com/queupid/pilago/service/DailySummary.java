package com.queupid.pilago.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.queupid.pilago.model.document.DocumentType;

/**
 * Today's numbers for the secretary's summary cards. Read-only; build a new one each time.
 * A plain class with getters so templates can write {@code ${summary.totalRequests}}.
 */
public final class DailySummary {

    private final LocalDate date;
    private final int totalRequests;
    private final int waiting;
    private final int onHold;
    private final int forSignature;
    private final int ready;
    private final int released;
    private final int skipped;
    private final BigDecimal feesCollected;
    private final Map<DocumentType, Integer> requestsByDocument;

    public DailySummary(LocalDate date, int totalRequests, int waiting, int onHold, int forSignature,
            int ready, int released, int skipped, BigDecimal feesCollected,
            Map<DocumentType, Integer> requestsByDocument) {
        this.date = date;
        this.totalRequests = totalRequests;
        this.waiting = waiting;
        this.onHold = onHold;
        this.forSignature = forSignature;
        this.ready = ready;
        this.released = released;
        this.skipped = skipped;
        this.feesCollected = feesCollected;
        this.requestsByDocument = Map.copyOf(requestsByDocument);
    }

    public LocalDate getDate() {
        return date;
    }

    /** Every ticket issued today, whatever its status. */
    public int getTotalRequests() {
        return totalRequests;
    }

    public int getWaiting() {
        return waiting;
    }

    public int getOnHold() {
        return onHold;
    }

    public int getForSignature() {
        return forSignature;
    }

    public int getReady() {
        return ready;
    }

    public int getReleased() {
        return released;
    }

    public int getSkipped() {
        return skipped;
    }

    /** Sum of fees marked paid today. */
    public BigDecimal getFeesCollected() {
        return feesCollected;
    }

    public Map<DocumentType, Integer> getRequestsByDocument() {
        return requestsByDocument;
    }
}
