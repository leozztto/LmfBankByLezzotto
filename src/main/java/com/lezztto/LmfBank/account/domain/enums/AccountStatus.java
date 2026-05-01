package com.lezztto.LmfBank.account.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum AccountStatus {

    ACTIVE("A", "account active"),
    BLOCKED("B", "account blocked"),
    CLOSED("C", "account closed");

    private final String code;
    private final String description;

    AccountStatus(String code, String description) {
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
    public static AccountStatus fromCode(String code) {
        for (AccountStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid AccountStatus: " + code);
    }
}
