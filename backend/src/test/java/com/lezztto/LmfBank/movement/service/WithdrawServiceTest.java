package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Regras de DÉBITO do ledger (saída de valor via saque), incluindo a
 * validação de saldo suficiente.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawServiceTest {

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
    private BalanceCalculatorService balanceCalculator;
    @Mock
    private BalanceProjectionService balanceProjectionService;

    @InjectMocks
    private WithdrawService withdrawService;

    private TransactionRequest debitRequest(BigDecimal amount) {
        return TransactionRequest.builder()
                .accountId(ACCOUNT_ID)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .description("Withdraw")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    private Account activeAccount() {
        return Account.builder().id(ACCOUNT_ID).accountStatus(AccountStatus.ACTIVE).build();
    }

    private Transaction persistedDebit(BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .build();
    }

    @Test
    @DisplayName("saldo suficiente: gera transação de DÉBITO e retorna a resposta mapeada")
    void shouldCreateDebitWhenBalanceIsSufficient() {
        BigDecimal amount = new BigDecimal("80.00");
        Transaction persisted = persistedDebit(amount);
        TransactionResponse expected = TransactionResponse.builder().transactionId(persisted.getId()).build();

        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(activeAccount());
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("100.00"));
        when(transactionDomainService.create(eq(ACCOUNT_ID), eq(TransactionType.DEBIT), eq(amount), eq("Withdraw"), anyString(), isNull()))
                .thenReturn(persisted);
        when(transactionMapper.toResponse(persisted)).thenReturn(expected);

        TransactionResponse result = withdrawService.process(debitRequest(amount));

        assertThat(result).isSameAs(expected);
        verify(transactionDomainService).create(eq(ACCOUNT_ID), eq(TransactionType.DEBIT), eq(amount), eq("Withdraw"), anyString(), isNull());
        verify(balanceProjectionService).refresh(ACCOUNT_ID);
    }

    @Test
    @DisplayName("valor exatamente igual ao saldo disponível é permitido")
    void shouldAllowWithdrawEqualToBalance() {
        BigDecimal amount = new BigDecimal("100.00");
        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(activeAccount());
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("100.00"));
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedDebit(amount));

        withdrawService.process(debitRequest(amount));

        verify(transactionDomainService).create(eq(ACCOUNT_ID), eq(TransactionType.DEBIT), eq(amount), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("valor acima do saldo: lança InsufficientBalanceException e não gera transação nem atualiza projeção")
    void shouldRejectWithdrawAboveBalance() {
        BigDecimal amount = new BigDecimal("100.01");
        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(activeAccount());
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("100.00"));

        assertThatThrownBy(() -> withdrawService.process(debitRequest(amount)))
                .isInstanceOf(InsufficientBalanceException.class)
                .satisfies(ex -> {
                    InsufficientBalanceException ibe = (InsufficientBalanceException) ex;
                    assertThat(ibe.getAccountId()).isEqualTo(ACCOUNT_ID);
                    assertThat(ibe.getCurrentBalance()).isEqualByComparingTo("100.00");
                    assertThat(ibe.getRequestedAmount()).isEqualByComparingTo("100.01");
                });

        verifyNoInteractions(transactionDomainService);
        verifyNoInteractions(balanceProjectionService);
    }

    @Test
    @DisplayName("saldo negativo (conta já devedora): qualquer débito é rejeitado")
    void shouldRejectWithdrawWhenBalanceIsNegative() {
        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(activeAccount());
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("-5.00"));

        assertThatThrownBy(() -> withdrawService.process(debitRequest(new BigDecimal("0.01"))))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("valida o status da conta antes de checar saldo")
    void shouldValidateAccountStatusBeforeBalance() {
        Account closed = Account.builder().id(ACCOUNT_ID).accountStatus(AccountStatus.CLOSED).build();
        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(closed);
        doThrow(new AccountStatusException(ACCOUNT_ID, AccountStatus.CLOSED.name()))
                .when(accountValidator)
                .validateStatusAccountForTransaction(ACCOUNT_ID, AccountStatus.CLOSED.name());

        assertThatThrownBy(() -> withdrawService.process(debitRequest(BigDecimal.ONE)))
                .isInstanceOf(AccountStatusException.class);

        verifyNoInteractions(balanceCalculator);
        verifyNoInteractions(transactionDomainService);
    }

    @Test
    @DisplayName("nunca gera transação de CRÉDITO no fluxo de saque")
    void shouldNeverCreateCredit() {
        when(accountService.findByIdAccountForUpdate(ACCOUNT_ID)).thenReturn(activeAccount());
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("1000.00"));
        when(transactionDomainService.create(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(persistedDebit(BigDecimal.TEN));

        withdrawService.process(debitRequest(BigDecimal.TEN));

        verify(transactionDomainService, never())
                .create(any(), eq(TransactionType.CREDIT), any(), anyString(), anyString(), any());
    }
}
