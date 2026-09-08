package com.lezztto.LmfBank.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-with-more-than-32-bytes!!";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
    }

    @Test
    @DisplayName("generateToken + extractUsername faz o round-trip do subject")
    void roundTripsUsername() {
        String token = jwtService.generateToken("charlie");

        assertThat(jwtService.extractUsername(token)).isEqualTo("charlie");
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    @DisplayName("isValid: false para string que não é JWT")
    void invalidForGarbage() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("getExpiresInSeconds converte a expiração em ms para segundos")
    void expiresInSeconds() {
        assertThat(jwtService.getExpiresInSeconds()).isEqualTo(3_600L);
    }

    @Test
    @DisplayName("isValid: false para token assinado com outra chave")
    void invalidForWrongSignature() {
        JwtService other = new JwtService();
        ReflectionTestUtils.setField(other, "secret", "another-secret-key-with-more-than-32-bytes!!");
        ReflectionTestUtils.setField(other, "expiration", 3_600_000L);
        String foreignToken = other.generateToken("mallory");

        assertThat(jwtService.isValid(foreignToken)).isFalse();
        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken)).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("isValid: false para token expirado")
    void invalidForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L);
        String expired = jwtService.generateToken("dave");

        assertThat(jwtService.isValid(expired)).isFalse();
    }
}
