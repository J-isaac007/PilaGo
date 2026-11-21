package com.queupid.pilago.model.person;

import com.queupid.pilago.exception.NotAResidentException;
import com.queupid.pilago.model.request.HoldReason;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * A resident who requests a document. Residents do not log in; they track their
 * ticket through a private link.
 */
@Entity
@DiscriminatorValue("RESIDENT")
public class Resident extends Person {

    /** Seniors are 60 or older. */
    public static final int SENIOR_AGE = 60;

    /** Residents who lived in the barangay for less than this are put on hold. */
    public static final int MIN_RESIDENCY_MONTHS = 6;

    /*
     * Wrapper types on purpose: all people share one table, and staff rows leave these columns empty.
     * With int/boolean, Hibernate adds a table check rule that H2 rejects for every resident insert.
     * The getters below still return plain int/boolean, so the rest of the code never sees a null.
     */
    private Integer residencyMonths;
    private Boolean pwd;
    private Boolean hasPendingRecord;

    /** For JPA only. */
    protected Resident() {
    }

    public Resident(String name, int age, String address, int residencyMonths, boolean pwd, boolean hasPendingRecord) {
        super(name, age, address);
        setResidencyMonths(residencyMonths);
        this.pwd = pwd;
        this.hasPendingRecord = hasPendingRecord;
    }

    @Override
    public String getRole() {
        return "RESIDENT";
    }

    public boolean isSenior() {
        return getAge() >= SENIOR_AGE;
    }

    /** Senior wins over PWD when a resident is both: the ticket still starts with S. */
    public PriorityGroup getPriorityGroup() {
        if (isSenior()) {
            return PriorityGroup.SENIOR;
        }
        return isPwd() ? PriorityGroup.PWD : PriorityGroup.REGULAR;
    }

    /**
     * Residency verification from the proposal.
     *
     * @throws NotAResidentException if the resident lived here less than six months or declared a
     *         pending barangay record. The request is then put on hold for the secretary to check.
     */
    public void checkResidency() throws NotAResidentException {
        if (getResidencyMonths() < MIN_RESIDENCY_MONTHS) {
            throw new NotAResidentException(HoldReason.SHORT_RESIDENCY);
        }
        if (hasPendingRecord()) {
            throw new NotAResidentException(HoldReason.PENDING_RECORD);
        }
    }

    public int getResidencyMonths() {
        return residencyMonths == null ? 0 : residencyMonths;
    }

    public void setResidencyMonths(int residencyMonths) {
        if (residencyMonths < 0) {
            throw new IllegalArgumentException("Months of residency cannot be negative");
        }
        this.residencyMonths = residencyMonths;
    }

    /** Whole years of residency, for display ("3 yrs 4 mos"). */
    public int getResidencyYearsPart() {
        return getResidencyMonths() / 12;
    }

    /** Leftover months after whole years, for display. */
    public int getResidencyMonthsPart() {
        return getResidencyMonths() % 12;
    }

    public boolean isPwd() {
        return Boolean.TRUE.equals(pwd);
    }

    public void setPwd(boolean pwd) {
        this.pwd = pwd;
    }

    public boolean hasPendingRecord() {
        return Boolean.TRUE.equals(hasPendingRecord);
    }

    public void setHasPendingRecord(boolean hasPendingRecord) {
        this.hasPendingRecord = hasPendingRecord;
    }
}
