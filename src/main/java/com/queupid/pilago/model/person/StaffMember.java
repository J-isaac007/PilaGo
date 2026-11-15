package com.queupid.pilago.model.person;

import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * A barangay staff member who logs in with a Staff ID. Each subclass lists the actions it may
 * perform (polymorphism); Spring Security uses {@link #getRole()} as the role name.
 */
@Entity
public abstract class StaffMember extends Person {

    @Column(unique = true)
    private String staffId;

    /** BCrypt hash, never the plain password. */
    private String passwordHash;

    /** For JPA only. */
    protected StaffMember() {
    }

    protected StaffMember(String name, int age, String address, String staffId, String passwordHash) {
        super(name, age, address);
        if (staffId == null || staffId.isBlank()) {
            throw new IllegalArgumentException("Staff ID is required");
        }
        this.staffId = staffId.strip();
        this.passwordHash = passwordHash;
    }

    /** What this staff member is allowed to do. */
    public abstract Set<StaffAction> getAllowedActions();

    public boolean canPerform(StaffAction action) {
        return getAllowedActions().contains(action);
    }

    public String getStaffId() {
        return staffId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void changePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        this.passwordHash = newPasswordHash;
    }
}
