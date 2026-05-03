package com.lezztto.LmfBank.account.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static AccountStatus fromCode(String code) {
        if (code == null) return null;

        for (AccountStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid AccountStatus: " + code);
    }
}
