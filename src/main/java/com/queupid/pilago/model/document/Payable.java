package com.queupid.pilago.model.document;

import java.math.BigDecimal;

/** Abstraction: anything that has a fee must be able to compute it. */
public interface Payable {

    /** The fee in pesos. {@link BigDecimal#ZERO} means free. */
    BigDecimal computeFee();

    default boolean isFree() {
        return computeFee().signum() == 0;
    }
}
