package com.lezztto.LmfBank.auth.controller;

import com.lezztto.LmfBank.auth.domain.LoginRequest;
import com.lezztto.LmfBank.auth.domain.LoginResponse;
import com.lezztto.LmfBank.auth.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("login devolve token, tipo Bearer e o TTL em segundos")
    void loginReturnsLoginResponse() {
        when(jwtService.generateToken("bob")).thenReturn("signed.jwt.token");
        when(jwtService.getExpiresInSeconds()).thenReturn(86400L);

        LoginResponse response = authController.login(new LoginRequest("bob", "secret"));

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400L);
    }
}
