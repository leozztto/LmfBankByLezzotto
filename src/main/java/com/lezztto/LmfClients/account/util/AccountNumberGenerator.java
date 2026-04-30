package com.lezztto.LmfClients.account.util;

public class AccountNumberGenerator {

    public static String generateAccountNumber() {

        String baseNumber = generateBaseNumber(8); // 8 dígitos
        int dv = calculateDV(baseNumber);

        return baseNumber + "-" + dv;
    }

    private static String generateBaseNumber(int length) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            sb.append((int) (Math.random() * 10));
        }

        return sb.toString();
    }

    private static int calculateDV(String number) {
        int weight = 2;
        int sum = 0;

        for (int i = number.length() - 1; i >= 0; i--) {
            int num = Character.getNumericValue(number.charAt(i));
            sum += num * weight;
            weight++;
            if (weight > 9) weight = 2;
        }

        int mod = sum % 11;
        int dv = 11 - mod;

        if (dv >= 10) {
            return 0;
        }

        return dv;
    }
}
