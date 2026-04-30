package com.lezztto.LmfClients.account.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountType {

    CHECKING("C", "Checking Account"),
    SAVINGS("S", "Savings Account");

    private final String code;
    private final String description;

    AccountType(String code, String description) {
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
    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AccountType: " + code);
    }
}
