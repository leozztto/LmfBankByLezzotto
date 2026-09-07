package com.lezztto.LmfBank.account.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mascaramento de dados sensíveis exibidos nas respostas de conta.
 */
class MaskUtilsTest {

    @Nested
    @DisplayName("maskDocument")
    class MaskDocument {

        @Test
        @DisplayName("mantém os dígitos do meio e do fim, oculta o resto")
        void masksValidDocument() {
            assertThat(MaskUtils.maskDocument("12345678901")).isEqualTo("***.456.***-01");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "123", "123456789012", "abc"})
        @DisplayName("devolve o valor original quando não tem 11 caracteres")
        void returnsInputWhenNotElevenChars(String input) {
            assertThat(MaskUtils.maskDocument(input)).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("maskEmail")
    class MaskEmail {

        @Test
        @DisplayName("mantém as 2 primeiras letras do usuário e o domínio")
        void masksNormalEmail() {
            assertThat(MaskUtils.maskEmail("leandro@example.com")).isEqualTo("le*****@example.com");
        }

        @Test
        @DisplayName("usuário com 2 caracteres ou menos vira **")
        void masksShortLocalPart() {
            assertThat(MaskUtils.maskEmail("ab@example.com")).isEqualTo("**@example.com");
            assertThat(MaskUtils.maskEmail("a@x.io")).isEqualTo("**@x.io");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "no-at-sign", "plain text"})
        @DisplayName("devolve o valor original quando não é e-mail")
        void returnsInputWhenNotEmail(String input) {
            assertThat(MaskUtils.maskEmail(input)).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("maskPhone")
    class MaskPhone {

        @Test
        @DisplayName("mantém DDD e os 4 últimos dígitos")
        void masksValidPhone() {
            assertThat(MaskUtils.maskPhone("11987654321")).isEqualTo("(11) *****-4321");
        }

        @Test
        @DisplayName("aceita telefone com exatamente 10 caracteres")
        void masksTenCharPhone() {
            assertThat(MaskUtils.maskPhone("1133334444")).isEqualTo("(11) *****-4444");
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {"", "12345", "999999999"})
        @DisplayName("devolve o valor original quando tem menos de 10 caracteres")
        void returnsInputWhenTooShort(String input) {
            assertThat(MaskUtils.maskPhone(input)).isEqualTo(input);
        }
    }
}
