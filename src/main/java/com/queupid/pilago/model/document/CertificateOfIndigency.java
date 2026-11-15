package com.queupid.pilago.model.document;

import java.math.BigDecimal;
import java.util.List;

/**
 * Always free. The proposal's Features section says free and its OOP section says ₱50;
 * the team chose free.
 */
public class CertificateOfIndigency extends Document {

    public CertificateOfIndigency(Purpose purpose) {
        super(DocumentType.CERTIFICATE_OF_INDIGENCY, BigDecimal.ZERO, purpose);
    }

    @Override
    public BigDecimal getFee(Purpose purpose) {
        return BigDecimal.ZERO;
    }

    @Override
    public List<String> getRequirements() {
        return List.of("requirement.valid-id");
    }

    @Override
    public int getProcessingTime() {
        return 6;
    }
}
