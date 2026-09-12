package com.lezztto.LmfBank.account.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Geração do número de conta e do dígito verificador (módulo 11).
 */
class AccountNumberGeneratorTest {

    private static int calculateDV(String base) {
        try {
            Method m = AccountNumberGenerator.class.getDeclaredMethod("calculateDV", String.class);
            m.setAccessible(true);
            return (int) m.invoke(null, base);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Independent re-implementation of the mod-11 check digit, for cross-checking. */
    private static int expectedDv(String base) {
        int weight = 2, sum = 0;
        for (int i = base.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(base.charAt(i)) * weight;
            weight = weight == 9 ? 2 : weight + 1;
        }
        int dv = 11 - (sum % 11);
        return dv >= 10 ? 0 : dv;
    }

    @RepeatedTest(20)
    @DisplayName("formato 8 dígitos + '-' + dígito verificador, com DV consistente")
    void generatesWellFormedNumberWithValidCheckDigit() {
        String number = AccountNumberGenerator.generateAccountNumber();

        assertThat(number).matches("\\d{8}-\\d");

        String base = number.substring(0, 8);
        int dv = Integer.parseInt(number.substring(9));
        assertThat(dv).isBetween(0, 9);
        assertThat(dv).isEqualTo(expectedDv(base));
    }

    @Test
    @DisplayName("DV de um número conhecido")
    void checkDigitOfKnownBase() {
        assertThat(calculateDV("12345678")).isEqualTo(expectedDv("12345678"));
    }

    @Test
    @DisplayName("quando 11 - (sum % 11) daria 10 ou 11, o DV vira 0")
    void checkDigitCollapsesToZero() {
        // sum % 11 == 0  -> 11 - 0 == 11 -> 0
        assertThat(calculateDV("00000000")).isZero();
        // sum % 11 == 1  -> 11 - 1 == 10 -> 0  (rightmost digit 6, weight 2 -> sum 12)
        assertThat(calculateDV("00000006")).isZero();
    }

    @Test
    @DisplayName("gera números diferentes em chamadas sucessivas")
    void generatesDistinctNumbers() {
        String a = AccountNumberGenerator.generateAccountNumber();
        String b = AccountNumberGenerator.generateAccountNumber();
        // Not strictly guaranteed, but a collision across 10^8 space is negligible.
        assertThat(a).isNotEqualTo(b);
    }
}
