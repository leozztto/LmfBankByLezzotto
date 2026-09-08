package com.lezztto.LmfBank.movement.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TransactionType {

    CREDIT("C", "Credito"),
    DEBIT("D", "Debito");

    private final String code;
    private final String description;

    TransactionType(String code, String description) {
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
    public static TransactionType fromCode(String code) {
        if (code == null) return null;

        for (TransactionType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid TransactionType: " + code);
    }
}
