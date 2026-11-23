package com.queupid.pilago.service;

import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * What the resident fills in across the three request steps. Bind it with
 * {@code th:object="${form}"} and {@code @Valid @ModelAttribute RequestForm form}.
 *
 * <p>Residency is entered as years plus months; {@link #getTotalResidencyMonths()} adds them up.
 * Letters typed into a number field fail binding before validation runs, so the controller should
 * also check {@code BindingResult} for type mismatches (shown as "Numbers only").
 */
public class RequestForm {

    @NotNull(message = "{form.document.required}")
    private DocumentType documentType;

    @NotNull(message = "{form.purpose.required}")
    private Purpose purpose;

    @NotBlank(message = "{form.name.required}")
    @Size(max = 120, message = "{form.name.too-long}")
    private String fullName;

    @NotNull(message = "{form.age.required}")
    @Min(value = 1, message = "{form.age.range}")
    @Max(value = 130, message = "{form.age.range}")
    private Integer age;

    @NotBlank(message = "{form.address.required}")
    @Size(max = 200, message = "{form.address.too-long}")
    private String address;

    @NotNull(message = "{form.residency.required}")
    @Min(value = 0, message = "{form.residency.range}")
    @Max(value = 130, message = "{form.residency.range}")
    private Integer residencyYears;

    @NotNull(message = "{form.residency.required}")
    @Min(value = 0, message = "{form.residency.months-range}")
    @Max(value = 11, message = "{form.residency.months-range}")
    private Integer residencyMonths;

    private boolean pwd;
    private boolean hasPendingRecord;

    public RequestForm() {
    }

    /** Convenience constructor for tests and sample data. */
    public RequestForm(DocumentType documentType, Purpose purpose, String fullName, int age, String address,
            int residencyYears, int residencyMonths, boolean pwd, boolean hasPendingRecord) {
        this.documentType = documentType;
        this.purpose = purpose;
        this.fullName = fullName;
        this.age = age;
        this.address = address;
        this.residencyYears = residencyYears;
        this.residencyMonths = residencyMonths;
        this.pwd = pwd;
        this.hasPendingRecord = hasPendingRecord;
    }

    public int getTotalResidencyMonths() {
        int years = residencyYears == null ? 0 : residencyYears;
        int months = residencyMonths == null ? 0 : residencyMonths;
        return years * 12 + months;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(Purpose purpose) {
        this.purpose = purpose;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getResidencyYears() {
        return residencyYears;
    }

    public void setResidencyYears(Integer residencyYears) {
        this.residencyYears = residencyYears;
    }

    public Integer getResidencyMonths() {
        return residencyMonths;
    }

    public void setResidencyMonths(Integer residencyMonths) {
        this.residencyMonths = residencyMonths;
    }

    public boolean isPwd() {
        return pwd;
    }

    public void setPwd(boolean pwd) {
        this.pwd = pwd;
    }

    public boolean isHasPendingRecord() {
        return hasPendingRecord;
    }

    public void setHasPendingRecord(boolean hasPendingRecord) {
        this.hasPendingRecord = hasPendingRecord;
    }
}
