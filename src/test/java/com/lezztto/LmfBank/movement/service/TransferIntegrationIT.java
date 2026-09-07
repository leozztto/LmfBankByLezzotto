package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.exception.AccountStatusException;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for the TRANSFER rule: the atomic DEBIT + CREDIT pair, its rollback
 * on failure, and transfer-level idempotency, all against a real database.
 */
class TransferIntegrationIT extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    private TransferRequest request(Long from, Long to, String amount, UUID key) {
        return TransferRequest.builder()
                .fromAccountId(from)
                .toAccountId(to)
                .amount(new BigDecimal(amount))
                .idempotencyKey(key)
                .build();
    }

    @Test
    @DisplayName("caminho feliz: debita origem, credita destino e grava a transferência como COMPLETED")
    void shouldMoveMoneyBetweenAccounts() {
        Long from = persistAccountWithBalance("500.00");
        Long to = persistActiveAccount();

        TransferResponse response =
                transferService.createTransfer(request(from, to, "120.00", UUID.randomUUID()));

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(response.getTransferId()).isNotNull();

        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("380.00");
        assertThat(ledgerBalanceOf(to)).isEqualByComparingTo("120.00");

        Transaction debit = transactionRepository.findByAccountIdAndType(from, TransactionType.DEBIT).get(0);
        Transaction credit = transactionRepository.findByAccountIdAndType(to, TransactionType.CREDIT).get(0);

        assertThat(debit.getAmount()).isEqualByComparingTo("120.00");
        assertThat(credit.getAmount()).isEqualByComparingTo("120.00");
        assertThat(debit.getTransferId())
                .isNotNull()
                .isEqualTo(credit.getTransferId())
                .isEqualTo(response.getTransferId());
    }

    @Test
    @DisplayName("saldo insuficiente: lança InsufficientBalanceException e não grava transferência nem lançamentos")
    void shouldRejectAndRollbackWhenInsufficientBalance() {
        Long from = persistAccountWithBalance("10.00");
        Long to = persistActiveAccount();

        assertThatThrownBy(() ->
                transferService.createTransfer(request(from, to, "10.01", UUID.randomUUID())))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(transferRepository.count()).isZero();
        assertThat(transactionRepository.findByAccountIdAndType(from, TransactionType.DEBIT)).isEmpty();
        assertThat(transactionRepository.findByAccountIdAndType(to, TransactionType.CREDIT)).isEmpty();
        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("origem igual ao destino: lança IllegalArgumentException")
    void shouldRejectTransferToSameAccount() {
        Long account = persistAccountWithBalance("100.00");

        assertThatThrownBy(() ->
                transferService.createTransfer(request(account, account, "10.00", UUID.randomUUID())))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    @DisplayName("conta de destino bloqueada: lança AccountStatusException e faz rollback")
    void shouldRejectWhenDestinationBlocked() {
        Long from = persistAccountWithBalance("100.00");
        Long to = persistAccount(AccountStatus.BLOCKED);

        assertThatThrownBy(() ->
                transferService.createTransfer(request(from, to, "10.00", UUID.randomUUID())))
                .isInstanceOf(AccountStatusException.class);

        assertThat(transactionRepository.findByAccountIdAndType(from, TransactionType.DEBIT)).isEmpty();
        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("mesma idempotencyKey: a segunda chamada devolve a transferência existente sem duplicar lançamentos")
    void shouldBeIdempotentOnIdempotencyKey() {
        Long from = persistAccountWithBalance("500.00");
        Long to = persistActiveAccount();
        UUID key = UUID.randomUUID();

        TransferResponse first = transferService.createTransfer(request(from, to, "75.00", key));
        TransferResponse second = transferService.createTransfer(request(from, to, "75.00", key));

        assertThat(second.getTransferId()).isNotNull().isEqualTo(first.getTransferId());
        assertThat(transferRepository.count()).isEqualTo(1);
        assertThat(transactionRepository.findByAccountIdAndType(from, TransactionType.DEBIT)).hasSize(1);
        assertThat(transactionRepository.findByAccountIdAndType(to, TransactionType.CREDIT)).hasSize(1);
        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("425.00");
    }

    @Test
    @DisplayName("transferências sequenciais consomem o saldo até esgotar")
    void shouldConsumeBalanceAcrossSequentialTransfers() {
        Long from = persistAccountWithBalance("100.00");
        Long to = persistActiveAccount();

        transferService.createTransfer(request(from, to, "60.00", UUID.randomUUID()));

        assertThatThrownBy(() ->
                transferService.createTransfer(request(from, to, "50.00", UUID.randomUUID())))
                .isInstanceOf(InsufficientBalanceException.class);

        List<Transaction> debits = transactionRepository.findByAccountIdAndType(from, TransactionType.DEBIT);
        assertThat(debits).hasSize(1);
        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("40.00");
    }
}
