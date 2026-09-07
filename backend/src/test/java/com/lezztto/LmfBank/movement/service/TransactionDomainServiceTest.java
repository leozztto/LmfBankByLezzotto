package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de criação de um lançamento individual do ledger (uma "perna" de
 * crédito ou débito), com idempotência por transferId + tipo.
 */
@ExtendWith(MockitoExtension.class)
class TransactionDomainServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionDomainService transactionDomainService;

    @Test
    @DisplayName("sem lançamento anterior: persiste nova transação COMPLETED com os dados informados")
    void shouldPersistNewTransaction() {
        UUID transferId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.00");
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionDomainService.create(
                ACCOUNT_ID, TransactionType.CREDIT, amount, "Deposit", transferId);

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getDescription()).isEqualTo("Deposit");
        assertThat(result.getTransferId()).isEqualTo(transferId);
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("chave de idempotência é composta por transferId + nome do tipo")
    void shouldBuildIdempotencyKeyFromTransferIdAndType() {
        UUID transferId = UUID.randomUUID();
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionDomainService.create(ACCOUNT_ID, TransactionType.DEBIT, BigDecimal.TEN, "Transfer", transferId);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(transactionRepository).findByIdempotencyKey(key.capture());
        assertThat(key.getValue()).isEqualTo(transferId + "-DEBIT");
    }

    @Test
    @DisplayName("lançamento já existente para a chave: devolve o existente e não persiste de novo")
    void shouldReturnExistingTransactionAndNotPersist() {
        UUID transferId = UUID.randomUUID();
        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .type(TransactionType.CREDIT)
                .amount(BigDecimal.TEN)
                .status(TransactionStatus.COMPLETED)
                .transferId(transferId)
                .idempotencyKey(transferId + "-CREDIT")
                .build();
        when(transactionRepository.findByIdempotencyKey(transferId + "-CREDIT")).thenReturn(Optional.of(existing));

        Transaction result = transactionDomainService.create(
                ACCOUNT_ID, TransactionType.CREDIT, BigDecimal.TEN, "Deposit", transferId);

        assertThat(result).isSameAs(existing);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("mesma transferência gera chaves distintas para a perna de débito e a de crédito")
    void shouldGenerateDistinctKeysPerLeg() {
        UUID transferId = UUID.randomUUID();
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionDomainService.create(ACCOUNT_ID, TransactionType.DEBIT, BigDecimal.TEN, "Transfer out", transferId);
        transactionDomainService.create(2L, TransactionType.CREDIT, BigDecimal.TEN, "Transfer in", transferId);

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(Transaction::getIdempotencyKey)
                .containsExactly(transferId + "-DEBIT", transferId + "-CREDIT");
    }
}
