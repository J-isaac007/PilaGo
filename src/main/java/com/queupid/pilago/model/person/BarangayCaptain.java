package com.queupid.pilago.model.person;

import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Signs documents before they are released. */
@Entity
@DiscriminatorValue("CAPTAIN")
public class BarangayCaptain extends StaffMember {

    private static final Set<StaffAction> ALLOWED = EnumSet.of(
            StaffAction.VIEW_FOR_SIGNATURE,
            StaffAction.SIGN,
            StaffAction.VIEW_DAILY_SUMMARY);

    /** For JPA only. */
    protected BarangayCaptain() {
    }

    public BarangayCaptain(String name, int age, String address, String staffId, String passwordHash) {
        super(name, age, address, staffId, passwordHash);
    }

    @Override
    public String getRole() {
        return "CAPTAIN";
    }

    @Override
    public Set<StaffAction> getAllowedActions() {
        return Set.copyOf(ALLOWED);
    }
}
