package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.exception.AccountAlreadyLinkedException;
import com.lezztto.LmfBank.auth.exception.AppUserNotFoundException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    @DisplayName("vincula a conta ao usuário quando os dois existem e a conta está livre")
    void linksAccountToUser() {
        AppUser demo = AppUser.builder().username("demo").role(Role.USER).build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(appUserRepository.existsByAccountId(10L)).thenReturn(false);

        appUserService.linkAccount("demo", 10L);

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getAccountId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("usuário inexistente -> AppUserNotFoundException, nada é salvo")
    void unknownUser() {
        when(appUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.linkAccount("ghost", 10L))
                .isInstanceOf(AppUserNotFoundException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("conta inexistente -> propaga AccountNotFoundException, nada é salvo")
    void unknownAccount() {
        AppUser demo = AppUser.builder().username("demo").role(Role.USER).build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(accountService.findByIdAccount(999L)).thenThrow(new AccountNotFoundException(999L));

        assertThatThrownBy(() -> appUserService.linkAccount("demo", 999L))
                .isInstanceOf(AccountNotFoundException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("conta já vinculada a outro usuário -> AccountAlreadyLinkedException")
    void alreadyLinkedAccount() {
        AppUser demo = AppUser.builder().username("demo").role(Role.USER).build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(appUserRepository.existsByAccountId(10L)).thenReturn(true);

        assertThatThrownBy(() -> appUserService.linkAccount("demo", 10L))
                .isInstanceOf(AccountAlreadyLinkedException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("corrida: existsByAccountId passa mas o UNIQUE do banco estoura -> AccountAlreadyLinkedException, não 500")
    void raceOnConcurrentLink() {
        AppUser demo = AppUser.builder().username("demo").role(Role.USER).build();
        when(appUserRepository.findByUsername("demo")).thenReturn(Optional.of(demo));
        when(appUserRepository.existsByAccountId(10L)).thenReturn(false);
        when(appUserRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> appUserService.linkAccount("demo", 10L))
                .isInstanceOf(AccountAlreadyLinkedException.class);
    }
}
