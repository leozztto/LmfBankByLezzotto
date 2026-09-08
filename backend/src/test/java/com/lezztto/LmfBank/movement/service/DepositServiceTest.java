package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Regras de CRÉDITO do ledger (entrada de valor via depósito).
 */
@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private AccountService accountService;
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private AccountValidator accountValidator;
    @Mock
    private TransactionDomainService transactionDomainService;
    @Mock
    private BalanceProjectionService balanceProjectionService;

    @InjectMocks
    private DepositService depositService;

    private TransactionRequest creditRequest(BigDecimal amount) {
        return TransactionRequest.builder()
                .accountId(ACCOUNT_ID)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .description("Deposit")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    private Account activeAccount() {
        return Account.builder()
                .id(ACCOUNT_ID)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
    }

    private Transaction persistedCredit(BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .build();
    }

    @Test
    @DisplayName("gera transação de CRÉDITO com o valor solicitado e retorna a resposta mapeada")
    void shouldCreateCreditTransaction() {
        BigDecimal amount = new BigDecimal("150.00");
        TransactionRequest request = creditRequest(amount);
        Transaction persisted = persistedCredit(amount);
        TransactionResponse expected = TransactionResponse.builder().transactionId(persisted.getId()).build();

        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
        when(transactionDomainService.create(eq(ACCOUNT_ID), eq(TransactionType.CREDIT), eq(amount), eq("Deposit"), eq(request.getIdempotencyKey()), isNull()))
                .thenReturn(persisted);
        when(transactionMapper.toResponse(persisted)).thenReturn(expected);

        TransactionResponse result = depositService.process(request);

        assertThat(result).isSameAs(expected);
        verify(transactionDomainService).create(eq(ACCOUNT_ID), eq(TransactionType.CREDIT), eq(amount), eq("Deposit"), eq(request.getIdempotencyKey()), isNull());
    }

    @Test
    @DisplayName("crédito não valida saldo: aceita depósito mesmo com conta zerada")
    void shouldNotCheckBalanceForCredit() {
        BigDecimal amount = new BigDecimal("10.00");
        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedCredit(amount));

        depositService.process(creditRequest(amount));

        // nenhum cálculo de saldo é acionado no fluxo de crédito
        verify(accountService).findByIdAccount(ACCOUNT_ID);
    }

    @Test
    @DisplayName("valida o status da conta antes de gerar a transação")
    void shouldValidateAccountStatus() {
        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedCredit(BigDecimal.TEN));

        depositService.process(creditRequest(BigDecimal.TEN));

        verify(accountValidator).validateStatusAccountForTransaction(ACCOUNT_ID, AccountStatus.ACTIVE.name());
    }

    @Test
    @DisplayName("conta inativa: propaga AccountStatusException e não gera transação nem atualiza projeção")
    void shouldFailWhenAccountNotActive() {
        Account blocked = Account.builder().id(ACCOUNT_ID).accountStatus(AccountStatus.BLOCKED).build();
        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(blocked);
        doThrow(new AccountStatusException(ACCOUNT_ID, AccountStatus.BLOCKED.name()))
                .when(accountValidator)
                .validateStatusAccountForTransaction(ACCOUNT_ID, AccountStatus.BLOCKED.name());

        assertThatThrownBy(() -> depositService.process(creditRequest(BigDecimal.TEN)))
                .isInstanceOf(AccountStatusException.class);

        verifyNoInteractions(transactionDomainService);
        verifyNoInteractions(balanceProjectionService);
    }

    @Test
    @DisplayName("atualiza a projeção de saldo da conta após o crédito")
    void shouldRefreshBalanceProjection() {
        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedCredit(BigDecimal.TEN));

        depositService.process(creditRequest(BigDecimal.TEN));

        verify(balanceProjectionService).refresh(ACCOUNT_ID);
    }

    @Nested
    @DisplayName("descrição e origem da transação de crédito")
    class Metadata {

        @Test
        @DisplayName("repassa a descrição e a chave de idempotência do request, sem transferId")
        void shouldForwardRequestDescriptionAndKey() {
            TransactionRequest request = creditRequest(BigDecimal.ONE);
            when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
            when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                    .thenReturn(persistedCredit(BigDecimal.ONE));

            depositService.process(request);

            ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
            verify(transactionDomainService).create(eq(ACCOUNT_ID), eq(TransactionType.CREDIT), any(BigDecimal.class),
                    description.capture(), key.capture(), isNull());

            assertThat(description.getValue()).isEqualTo(request.getDescription());
            assertThat(key.getValue()).isEqualTo(request.getIdempotencyKey());
        }
    }

    @Test
    @DisplayName("nunca gera transação de DÉBITO no fluxo de depósito")
    void shouldNeverCreateDebit() {
        when(accountService.findByIdAccount(ACCOUNT_ID)).thenReturn(activeAccount());
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedCredit(BigDecimal.TEN));

        depositService.process(creditRequest(BigDecimal.TEN));

        verify(transactionDomainService, never())
                .create(any(), eq(TransactionType.DEBIT), any(), anyString(), anyString(), any());
    }
}
