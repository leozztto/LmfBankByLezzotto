package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.auth.domain.entity.AppUser;
import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.exception.AccountAlreadyLinkedException;
import com.lezztto.LmfBank.auth.exception.AppUserNotFoundException;
import com.lezztto.LmfBank.auth.exception.UsernameAlreadyExistsException;
import com.lezztto.LmfBank.auth.repository.AppUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    @DisplayName("cria o usuário com role USER, senha hasheada e sem conta vinculada")
    void createsUser() {
        when(appUserRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        var response = appUserService.createUser("newbie", "secret");

        assertThat(response.username()).isEqualTo("newbie");
        assertThat(response.role()).isEqualTo(Role.USER);
        assertThat(response.accountId()).isNull();

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.USER);
    }

    @Test
    @DisplayName("username já existe -> UsernameAlreadyExistsException, nada é salvo")
    void createUserRejectsDuplicateUsername() {
        when(appUserRepository.existsByUsername("demo")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.createUser("demo", "secret"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(appUserRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("corrida: existsByUsername passa mas o UNIQUE do banco estoura -> UsernameAlreadyExistsException, não 500")
    void createUserRaceOnConcurrentUsername() {
        when(appUserRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(appUserRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"app_user_username_key\""));

        assertThatThrownBy(() -> appUserService.createUser("newbie", "secret"))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("violação de integridade que NÃO é a constraint esperada sobe como está, não vira 409 errado")
    void createUserDoesNotMisattributeUnrelatedConstraintViolation() {
        when(appUserRepository.existsByUsername("newbie")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        DataIntegrityViolationException unrelated =
                new DataIntegrityViolationException("null value in column \"role\" violates not-null constraint");
        when(appUserRepository.saveAndFlush(any())).thenThrow(unrelated);

        assertThatThrownBy(() -> appUserService.createUser("newbie", "secret"))
                .isSameAs(unrelated);
    }

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
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"app_user_account_id_key\""));

        assertThatThrownBy(() -> appUserService.linkAccount("demo", 10L))
                .isInstanceOf(AccountAlreadyLinkedException.class);
    }
}
