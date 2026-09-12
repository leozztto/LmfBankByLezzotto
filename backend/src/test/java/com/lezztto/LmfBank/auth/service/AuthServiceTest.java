package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.exception.InvalidCredentialsException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("usuário existente e senha correta -> não lança")
    void authenticateAcceptsMatchingCredentials() {
        AppUser user = AppUser.builder().username("demo").passwordHash("hash").build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("demo", "hash")).thenReturn(true);

        assertThatCode(() -> authService.authenticate("demo", "demo"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("senha errada -> InvalidCredentialsException")
    void authenticateRejectsWrongPassword() {
        AppUser user = AppUser.builder().username("demo").passwordHash("hash").build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate("demo", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("usuário inexistente -> InvalidCredentialsException, sem checar senha")
    void authenticateRejectsUnknownUsername() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("ghost", "whatever"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
