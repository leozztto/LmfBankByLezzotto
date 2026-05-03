package com.lezztto.LmfBank.account.util;

public final class MaskUtils {

    private MaskUtils() {
    }

    public static String maskDocument(String document) {
        if (document == null || document.length() != 11) {
            return document;
        }

        return "***."
                + document.substring(3, 6)
                + ".***-"
                + document.substring(9);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];

        if (name.length() <= 2) {
            return "**@" + domain;
        }

        return name.substring(0, 2)
                + "*".repeat(name.length() - 2)
                + "@"
                + domain;
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 10) {
            return phone;
        }

        String ddd = phone.substring(0, 2);
        String lastDigits = phone.substring(phone.length() - 4);

        return "(" + ddd + ") *****-" + lastDigits;
    }
}
