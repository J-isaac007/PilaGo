package com.queupid.pilago.model.document;

import java.math.BigDecimal;
import java.util.List;

/** ₱50.00 when used for a job application; free for every other purpose. */
public class BarangayClearance extends Document {

    public static final BigDecimal EMPLOYMENT_FEE = new BigDecimal("50.00");

    public BarangayClearance(Purpose purpose) {
        super(DocumentType.BARANGAY_CLEARANCE, EMPLOYMENT_FEE, purpose);
    }

    @Override
    public BigDecimal getFee(Purpose purpose) {
        return purpose == Purpose.EMPLOYMENT ? getBaseFee() : BigDecimal.ZERO;
    }

    @Override
    public List<String> getRequirements() {
        return List.of("requirement.valid-id", "requirement.cedula");
    }

    @Override
    public int getProcessingTime() {
        return 8;
    }
}
