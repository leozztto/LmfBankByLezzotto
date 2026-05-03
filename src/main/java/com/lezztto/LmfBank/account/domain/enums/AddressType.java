package com.lezztto.LmfBank.account.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AddressType {

    RESIDENTIAL("R", "Residencial"),
    COMMERCIAL("C", "Comercial"),
    BILLING("B", "Faturamento");

    private final String code;
    private final String description;

    AddressType(String code, String description) {
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
    public static AddressType fromCode(String code) {
        if (code == null) return null;

        for (AddressType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AddressType: " + code);
    }
}
