package com.queupid.pilago.model.document;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Abstraction: what every barangay document has. Each document type extends this class and
 * overrides {@link #getFee(Purpose)} and {@link #getRequirements()} (polymorphism), so the rest
 * of the code can work with a plain {@code Document} variable.
 *
 * <p>Documents are plain Java objects, not database rows. A request stores its
 * {@link DocumentType} and {@link Purpose} and rebuilds the document with
 * {@link DocumentType#create(Purpose)}.
 */
public abstract class Document implements Payable {

    private final DocumentType type;
    private final BigDecimal baseFee;
    private final Purpose purpose;

    protected Document(DocumentType type, BigDecimal baseFee, Purpose purpose) {
        this.type = Objects.requireNonNull(type, "type");
        this.baseFee = Objects.requireNonNull(baseFee, "baseFee");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
    }

    /** This document's fee rule for a given purpose. */
    public abstract BigDecimal getFee(Purpose purpose);

    /** Message keys of what the resident must bring, e.g. {@code requirement.valid-id}. */
    public abstract List<String> getRequirements();

    /** Rough minutes the secretary needs to prepare it, used for wait estimates. */
    public abstract int getProcessingTime();

    @Override
    public BigDecimal computeFee() {
        return getFee(purpose);
    }

    public DocumentType getType() {
        return type;
    }

    /** English name; the bilingual label is {@code getType().getMessageKey()}. */
    public String getDocumentName() {
        return type.getEnglishName();
    }

    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    public String toString() {
        return getDocumentName() + " (" + purpose + ")";
    }
}
