package com.queupid.pilago.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.queupid.pilago.model.document.BarangayClearance;
import com.queupid.pilago.model.document.CertificateOfIndigency;
import com.queupid.pilago.model.document.CertificateOfResidency;
import com.queupid.pilago.model.document.Document;
import com.queupid.pilago.model.document.DocumentType;
import com.queupid.pilago.model.document.Purpose;

class DocumentFeeTest {

    @Test
    void clearanceForEmploymentCostsFiftyPesos() {
        Document doc = DocumentType.BARANGAY_CLEARANCE.create(Purpose.EMPLOYMENT);
        assertThat(doc).isInstanceOf(BarangayClearance.class);
        assertThat(doc.computeFee()).isEqualByComparingTo("50.00");
        assertThat(doc.isFree()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Purpose.class, names = "EMPLOYMENT", mode = EnumSource.Mode.EXCLUDE)
    void clearanceForOtherPurposesIsFree(Purpose purpose) {
        assertThat(DocumentType.BARANGAY_CLEARANCE.create(purpose).computeFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @ParameterizedTest
    @EnumSource(Purpose.class)
    void indigencyAndResidencyAreAlwaysFree(Purpose purpose) {
        assertThat(DocumentType.CERTIFICATE_OF_INDIGENCY.create(purpose)).isInstanceOf(CertificateOfIndigency.class);
        assertThat(DocumentType.CERTIFICATE_OF_INDIGENCY.create(purpose).isFree()).isTrue();
        assertThat(DocumentType.CERTIFICATE_OF_RESIDENCY.create(purpose)).isInstanceOf(CertificateOfResidency.class);
        assertThat(DocumentType.CERTIFICATE_OF_RESIDENCY.create(purpose).isFree()).isTrue();
    }

    @Test
    void residencyAsksForProofOfAddress() {
        assertThat(DocumentType.CERTIFICATE_OF_RESIDENCY.create(Purpose.SCHOOL).getRequirements())
                .contains("requirement.proof-of-address");
    }
}
