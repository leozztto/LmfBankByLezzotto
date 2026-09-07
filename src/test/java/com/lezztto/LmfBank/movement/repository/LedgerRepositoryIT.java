package com.lezztto.LmfBank.movement.repository;

import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.support.AbstractRepositoryTest;
import com.lezztto.LmfBank.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Slice tests for the ledger persistence layer: the balance aggregation queries,
 * idempotency lookup and its unique constraint, and the pessimistic-lock query.
 */
class LedgerRepositoryIT extends AbstractRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private AccountRepository accountRepository;

    private Long accountId;

    @BeforeEach
    void persistAccount() {
        Account account = accountRepository.save(TestData.activeAccount());
        accountId = account.getId();
    }

    private Transaction tx(TransactionType type, String amount, String key) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(type)
                .amount(new BigDecimal(amount))
                .status(TransactionStatus.COMPLETED)
                .description(type.name())
                .createdAt(LocalDateTime.now())
                .idempotencyKey(key)
                .build();
    }

    @Test
    @DisplayName("sumCredits e sumDebits agregam apenas o tipo correspondente")
    void shouldAggregateCreditsAndDebitsByType() {
        transactionRepository.save(tx(TransactionType.CREDIT, "100.00", "c1"));
        transactionRepository.save(tx(TransactionType.CREDIT, "50.50", "c2"));
        transactionRepository.save(tx(TransactionType.DEBIT, "30.00", "d1"));

        assertThat(transactionRepository.sumCredits(accountId)).isEqualByComparingTo("150.50");
        assertThat(transactionRepository.sumDebits(accountId)).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("conta sem movimentação: as somas retornam 0 (COALESCE), não null")
    void shouldReturnZeroForAccountWithoutTransactions() {
        assertThat(transactionRepository.sumCredits(accountId)).isEqualByComparingTo("0");
        assertThat(transactionRepository.sumDebits(accountId)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("findByIdempotencyKey localiza a transação gravada")
    void shouldFindByIdempotencyKey() {
        Transaction saved = transactionRepository.save(tx(TransactionType.CREDIT, "10.00", "key-x"));

        assertThat(transactionRepository.findByIdempotencyKey("key-x"))
                .get()
                .extracting(Transaction::getId)
                .isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("a idempotency_key tem unique constraint: gravar chave repetida falha")
    void shouldRejectDuplicateIdempotencyKey() {
        transactionRepository.saveAndFlush(tx(TransactionType.CREDIT, "10.00", "dup"));

        assertThatThrownBy(() ->
                transactionRepository.saveAndFlush(tx(TransactionType.DEBIT, "10.00", "dup")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findByAccountIdAndType filtra por conta e tipo")
    void shouldFilterByAccountAndType() {
        transactionRepository.save(tx(TransactionType.CREDIT, "1.00", "f1"));
        transactionRepository.save(tx(TransactionType.DEBIT, "2.00", "f2"));

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.DEBIT))
                .singleElement()
                .extracting(Transaction::getAmount)
                .isEqualTo(new BigDecimal("2.00"));
    }

    @Test
    @DisplayName("findByIdForUpdate carrega a conta (SELECT ... FOR UPDATE)")
    void shouldLoadAccountForUpdate() {
        assertThat(accountRepository.findByIdForUpdate(accountId))
                .get()
                .extracting(Account::getId)
                .isEqualTo(accountId);
    }
}
