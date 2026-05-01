package com.lezztto.LmfBank.account.exception;

public class DocumentNumberDuplicateException extends RuntimeException {

    private final String documentNumber;

    public DocumentNumberDuplicateException(String documentNumber) {
        super(String.format(
                "The number of document: %s already has a registered account",
                documentNumber
        ));
        this.documentNumber = documentNumber;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }
}
