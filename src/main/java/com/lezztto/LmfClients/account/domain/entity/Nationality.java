package com.lezztto.LmfClients.account.domain.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Nationality {

    BRAZILIAN("BR", "Brasileiro"),
    FOREIGN("FR", "Estrangeiro");

    private final String code;
    private final String description;

    Nationality(String code, String description) {
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
    public static Nationality fromCode(String code) {
        for (Nationality n : values()) {
            if (n.code.equalsIgnoreCase(code)) {
                return n;
            }
        }
        throw new IllegalArgumentException("Invalid nationality: " + code);
    }
}
