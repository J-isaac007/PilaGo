package com.queupid.pilago.model.document;

import java.util.function.Function;

/** The three documents PilaGo issues, and a factory that builds the right {@link Document} subclass. */
public enum DocumentType {

    BARANGAY_CLEARANCE("Barangay Clearance", "document.clearance", BarangayClearance::new),
    CERTIFICATE_OF_INDIGENCY("Certificate of Indigency", "document.indigency", CertificateOfIndigency::new),
    CERTIFICATE_OF_RESIDENCY("Certificate of Residency", "document.residency", CertificateOfResidency::new);

    private final String englishName;
    private final String messageKey;
    private final Function<Purpose, Document> factory;

    DocumentType(String englishName, String messageKey, Function<Purpose, Document> factory) {
        this.englishName = englishName;
        this.messageKey = messageKey;
        this.factory = factory;
    }

    /** Builds the document for this type and purpose. */
    public Document create(Purpose purpose) {
        return factory.apply(purpose);
    }

    public String getEnglishName() {
        return englishName;
    }

    /** Key in messages.properties for the English text shown on the page. */
    public String getMessageKey() {
        return messageKey;
    }
}
