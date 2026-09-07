package com.lezztto.LmfBank.auth.controller;

import com.lezztto.LmfBank.auth.domain.LoginRequest;
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
    @DisplayName("login devolve o token gerado para o username informado")
    void loginReturnsGeneratedToken() {
        when(jwtService.generateToken("bob")).thenReturn("signed.jwt.token");

        String token = authController.login(new LoginRequest("bob", "secret"));

        assertThat(token).isEqualTo("signed.jwt.token");
    }
}
