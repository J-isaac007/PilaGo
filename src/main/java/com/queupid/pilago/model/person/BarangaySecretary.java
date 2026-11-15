package com.queupid.pilago.model.person;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Calls tickets, records requests and payments, forwards to the captain and releases documents. */
@Entity
@DiscriminatorValue("SECRETARY")
public class BarangaySecretary extends StaffMember {

    private static final Set<StaffAction> ALLOWED = EnumSet.of(
            StaffAction.CALL_NEXT,
            StaffAction.SKIP,
            StaffAction.RECORD_PAYMENT,
            StaffAction.FORWARD_TO_CAPTAIN,
            StaffAction.PUT_ON_HOLD,
            StaffAction.VERIFY,
            StaffAction.RELEASE,
            StaffAction.VIEW_DAILY_SUMMARY);

    /** For JPA only. */
    protected BarangaySecretary() {
    }

    public BarangaySecretary(String name, int age, String address, String staffId, String passwordHash) {
        super(name, age, address, staffId, passwordHash);
    }

    @Override
    public String getRole() {
        return "SECRETARY";
    }

    @Override
    public Set<StaffAction> getAllowedActions() {
        return Set.copyOf(ALLOWED);
    }
}
