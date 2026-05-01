package com.lezztto.LmfBank.movement.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionStatus {

    PENDING("P", "Pending transaction"),
    COMPLETED("C", "Complete transaction"),
    FAILED("F", "Failed transaction");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid TransactionStatus: " + code);
    }
}
