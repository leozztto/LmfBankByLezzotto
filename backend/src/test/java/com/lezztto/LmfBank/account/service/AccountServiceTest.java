package com.lezztto.LmfBank.account.service;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.entity.Address;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.domain.response.AccountResponse;
import com.lezztto.LmfBank.account.exception.AccountNotFoundException;
import com.lezztto.LmfBank.account.exception.DocumentNumberDuplicateException;
import com.lezztto.LmfBank.account.mapper.AccountMapper;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountMapper accountMapper;

    @InjectMocks
    private AccountService accountService;

    private AccountDto dto(String documentNumber) {
        return AccountDto.builder().documentNumber(documentNumber).fullName("Holder").build();
    }

    private Account accountWith(List<Address> addresses) {
        return Account.builder().id(1L).documentNumber("12345678901").addresses(addresses).build();
    }

    @Test
    @DisplayName("create: gera número, zera saldos, liga endereços e persiste")
    void createPersistsMappedAccount() {
        AccountDto dto = dto("12345678901");
        Address address = Address.builder().street("Rua A").build();
        Account mapped = accountWith(new ArrayList<>(List.of(address)));
        AccountResponse expected = AccountResponse.builder().accountId(1L).build();

        when(accountRepository.existsByDocumentNumber("12345678901")).thenReturn(false);
        when(accountMapper.toAccount(dto)).thenReturn(mapped);
        when(accountRepository.save(mapped)).thenReturn(mapped);
        when(accountMapper.toAccountResponse(mapped)).thenReturn(expected);

        AccountResponse result = accountService.create(dto);

        assertThat(result).isSameAs(expected);
        assertThat(dto.getAccountNumber()).matches("\\d{8}-\\d");
        assertThat(address.getAccount()).isSameAs(mapped);
        assertThat(mapped.getBalance()).isNotNull();
        assertThat(mapped.getBalance().getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mapped.getBalance().getBlockedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mapped.getBalance().getTotalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(mapped.getBalance().getAccount()).isSameAs(mapped);
    }

    @Test
    @DisplayName("create: documento já cadastrado lança DocumentNumberDuplicateException e não persiste")
    void createRejectsDuplicateDocument() {
        when(accountRepository.existsByDocumentNumber("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> accountService.create(dto("12345678901")))
                .isInstanceOf(DocumentNumberDuplicateException.class);

        verify(accountRepository, never()).save(any());
        verify(accountMapper, never()).toAccount(any());
    }

    @Test
    @DisplayName("create: lista de endereços nula não quebra o fluxo")
    void createHandlesNullAddresses() {
        AccountDto dto = dto("99999999999");
        Account mapped = accountWith(null);

        when(accountRepository.existsByDocumentNumber("99999999999")).thenReturn(false);
        when(accountMapper.toAccount(dto)).thenReturn(mapped);
        when(accountRepository.save(mapped)).thenReturn(mapped);
        when(accountMapper.toAccountResponse(mapped)).thenReturn(AccountResponse.builder().build());

        accountService.create(dto);

        verify(accountRepository).save(mapped);
        assertThat(mapped.getBalance()).isNotNull();
    }

    @Test
    @DisplayName("save: delega ao repositório")
    void saveDelegates() {
        Account account = Account.builder().id(7L).build();
        when(accountRepository.save(account)).thenReturn(account);

        assertThat(accountService.save(account)).isSameAs(account);
    }

    @Test
    @DisplayName("findById: devolve a resposta mapeada quando encontra")
    void findByIdReturnsMappedResponse() {
        Account account = Account.builder().id(3L).build();
        AccountResponse expected = AccountResponse.builder().accountId(3L).build();
        when(accountRepository.findByIdWithRelations(3L)).thenReturn(Optional.of(account));
        when(accountMapper.toAccountResponse(account)).thenReturn(expected);

        assertThat(accountService.findById(3L)).isSameAs(expected);
    }

    @Test
    @DisplayName("findById: lança AccountNotFoundException quando não encontra")
    void findByIdThrowsWhenMissing() {
        when(accountRepository.findByIdWithRelations(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(404L))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(accountMapper);
    }

    @Test
    @DisplayName("findByIdAccount: devolve a entidade / lança quando ausente")
    void findByIdAccount() {
        Account account = Account.builder().id(5L).build();
        when(accountRepository.findByIdWithRelations(5L)).thenReturn(Optional.of(account));
        assertThat(accountService.findByIdAccount(5L)).isSameAs(account);

        when(accountRepository.findByIdWithRelations(6L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountService.findByIdAccount(6L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("findByIdAccountForUpdate: usa a query com lock / lança quando ausente")
    void findByIdAccountForUpdate() {
        Account account = Account.builder().id(8L).build();
        when(accountRepository.findByIdForUpdate(8L)).thenReturn(Optional.of(account));
        assertThat(accountService.findByIdAccountForUpdate(8L)).isSameAs(account);

        when(accountRepository.findByIdForUpdate(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountService.findByIdAccountForUpdate(9L))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("findByDocumentNumber: devolve a resposta mapeada / lança quando ausente")
    void findByDocumentNumber() {
        Account account = Account.builder().documentNumber("12345678901").build();
        AccountResponse expected = AccountResponse.builder().build();
        when(accountRepository.findByDocumentNumberWithRelations("12345678901")).thenReturn(Optional.of(account));
        when(accountMapper.toAccountResponse(account)).thenReturn(expected);
        assertThat(accountService.findByDocumentNumber("12345678901")).isSameAs(expected);

        when(accountRepository.findByDocumentNumberWithRelations("000")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountService.findByDocumentNumber("000"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("create: aplica defaults de servidor (accountStatus=ACTIVE, agency=0001, createdAt limpo)")
    void createAppliesServerDefaults() {
        AccountDto dto = dto("22222222222");
        Account mapped = accountWith(new ArrayList<>());
        mapped.setCreatedAt(java.time.LocalDateTime.now());

        when(accountRepository.existsByDocumentNumber("22222222222")).thenReturn(false);
        when(accountMapper.toAccount(dto)).thenReturn(mapped);
        when(accountRepository.save(mapped)).thenReturn(mapped);
        when(accountMapper.toAccountResponse(mapped)).thenReturn(AccountResponse.builder().build());

        accountService.create(dto);

        assertThat(mapped.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(mapped.getAgency()).isEqualTo("0001");
        assertThat(mapped.getCreatedAt()).isNull();
    }

    @Test
    @DisplayName("create: valores enviados de accountStatus e agency prevalecem sobre os defaults")
    void createKeepsProvidedStatusAndAgency() {
        AccountDto dto = dto("33333333333");
        Account mapped = accountWith(new ArrayList<>());
        mapped.setAccountStatus(AccountStatus.BLOCKED);
        mapped.setAgency("4277");

        when(accountRepository.existsByDocumentNumber("33333333333")).thenReturn(false);
        when(accountMapper.toAccount(dto)).thenReturn(mapped);
        when(accountRepository.save(mapped)).thenReturn(mapped);
        when(accountMapper.toAccountResponse(mapped)).thenReturn(AccountResponse.builder().build());

        accountService.create(dto);

        assertThat(mapped.getAccountStatus()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(mapped.getAgency()).isEqualTo("4277");
    }

    @Test
    @DisplayName("findAll: mapeia cada conta para a resposta")
    void findAllMapsList() {
        Account a = Account.builder().id(1L).build();
        Account b = Account.builder().id(2L).build();
        when(accountRepository.findAllWithRelations()).thenReturn(List.of(a, b));
        when(accountMapper.toAccountResponse(a)).thenReturn(AccountResponse.builder().accountId(1L).build());
        when(accountMapper.toAccountResponse(b)).thenReturn(AccountResponse.builder().accountId(2L).build());

        List<AccountResponse> result = accountService.findAll();

        assertThat(result).extracting(AccountResponse::getAccountId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("create: o número gerado é propagado para a entidade salva")
    void createStampsGeneratedNumberOnDto() {
        AccountDto dto = dto("11111111111");
        Account mapped = accountWith(new ArrayList<>());
        when(accountRepository.existsByDocumentNumber("11111111111")).thenReturn(false);
        when(accountMapper.toAccount(any())).thenReturn(mapped);
        when(accountRepository.save(any())).thenReturn(mapped);
        when(accountMapper.toAccountResponse(any())).thenReturn(AccountResponse.builder().build());

        accountService.create(dto);

        ArgumentCaptor<AccountDto> captor = ArgumentCaptor.forClass(AccountDto.class);
        verify(accountMapper).toAccount(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).matches("\\d{8}-\\d");
    }
}
