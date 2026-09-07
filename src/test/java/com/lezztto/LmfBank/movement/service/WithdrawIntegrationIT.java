package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the DEBIT rule: withdrawal against a real database, including
 * the insufficient-balance check and its rollback.
 */
class WithdrawIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    private TransactionRequest debitRequest(Long accountId, String amount) {
        return TransactionRequest.builder()
                .accountId(accountId)
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal(amount))
                .description("Withdraw")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    @Test
    @DisplayName("saldo suficiente: grava DEBIT e reduz o saldo do ledger")
    void shouldRecordDebitAndReduceBalance() {
        Long accountId = persistAccountWithBalance("100.00");

        transactionService.create(debitRequest(accountId, "40.00"));

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.DEBIT))
                .singleElement()
                .satisfies(t -> assertThat(t.getAmount()).isEqualByComparingTo("40.00"));

        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("60.00");
        assertThat(accountRepository.findByIdWithRelations(accountId).orElseThrow()
                .getBalance().getAvailableBalance()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("valor igual ao saldo disponível é permitido e zera o ledger")
    void shouldAllowDebitEqualToBalance() {
        Long accountId = persistAccountWithBalance("100.00");

        transactionService.create(debitRequest(accountId, "100.00"));

        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("saldo insuficiente: lança InsufficientBalanceException e faz rollback (nenhum DEBIT gravado)")
    void shouldRejectAndRollbackWhenInsufficientBalance() {
        Long accountId = persistAccountWithBalance("30.00");

        assertThatThrownBy(() -> transactionService.create(debitRequest(accountId, "30.01")))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.DEBIT)).isEmpty();
        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("30.00");
    }
}
