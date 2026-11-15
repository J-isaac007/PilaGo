package com.queupid.pilago.model.document;

import java.math.BigDecimal;
import java.util.List;

/** Always free, but the resident must prove where they live. */
public class CertificateOfResidency extends Document {

    public CertificateOfResidency(Purpose purpose) {
        super(DocumentType.CERTIFICATE_OF_RESIDENCY, BigDecimal.ZERO, purpose);
    }

    @Override
    public BigDecimal getFee(Purpose purpose) {
        return BigDecimal.ZERO;
    }

    @Override
    public List<String> getRequirements() {
        return List.of("requirement.valid-id", "requirement.proof-of-address");
    }

    @Override
    public int getProcessingTime() {
        return 6;
    }
}
