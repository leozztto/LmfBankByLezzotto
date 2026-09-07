package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the CREDIT rule: deposit against a real database.
 */
class DepositIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    private TransactionRequest creditRequest(Long accountId, String amount) {
        return TransactionRequest.builder()
                .accountId(accountId)
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal(amount))
                .description("Deposit")
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
    }

    @Test
    @DisplayName("depósito grava transação CREDIT COMPLETED e aumenta o saldo do ledger")
    void shouldRecordCreditAndIncreaseBalance() {
        Long accountId = persistActiveAccount();

        TransactionResponse response = transactionService.create(creditRequest(accountId, "250.00"));

        assertThat(response.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.CREDIT))
                .singleElement()
                .satisfies(t -> {
                    assertThat(t.getAmount()).isEqualByComparingTo("250.00");
                    assertThat(t.getDescription()).isEqualTo("Deposit");
                });

        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("depósito atualiza a projeção account_balance.availableBalance")
    void shouldRefreshBalanceProjection() {
        Long accountId = persistActiveAccount();

        transactionService.create(creditRequest(accountId, "80.00"));
        transactionService.create(creditRequest(accountId, "20.00"));

        BigDecimal projected = accountRepository.findByIdWithRelations(accountId)
                .orElseThrow()
                .getBalance()
                .getAvailableBalance();

        assertThat(projected).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("conta bloqueada: lança AccountStatusException e não grava transação")
    void shouldRejectDepositOnBlockedAccount() {
        Long accountId = persistAccount(AccountStatus.BLOCKED);

        assertThatThrownBy(() -> transactionService.create(creditRequest(accountId, "10.00")))
                .isInstanceOf(AccountStatusException.class);

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.CREDIT)).isEmpty();
        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("0");
    }
}
