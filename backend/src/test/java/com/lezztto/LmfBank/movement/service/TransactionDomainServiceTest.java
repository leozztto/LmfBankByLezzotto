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
 * Regras de criação de um lançamento individual do ledger. A chave de idempotência
 * agora é fornecida pelo chamador (a string do request, para depósito/saque; a chave
 * derivada da transferência, para as pernas).
 */
@ExtendWith(MockitoExtension.class)
class TransactionDomainServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionDomainService transactionDomainService;

    @Test
    @DisplayName("sem lançamento anterior: persiste nova transação COMPLETED com os dados e a chave informados")
    void shouldPersistNewTransaction() {
        BigDecimal amount = new BigDecimal("75.00");
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction result = transactionDomainService.create(
                ACCOUNT_ID, TransactionType.CREDIT, amount, "aluguel", "client-key-1", null);

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(result.getAmount()).isEqualByComparingTo(amount);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.getDescription()).isEqualTo("aluguel");
        assertThat(result.getIdempotencyKey()).isEqualTo("client-key-1");
        assertThat(result.getTransferId()).isNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("lançamento já existente para a chave: devolve o existente e não persiste de novo")
    void shouldReturnExistingTransactionAndNotPersist() {
        Transaction existing = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(ACCOUNT_ID)
                .type(TransactionType.CREDIT)
                .amount(BigDecimal.TEN)
                .status(TransactionStatus.COMPLETED)
                .idempotencyKey("client-key-2")
                .build();
        when(transactionRepository.findByIdempotencyKey("client-key-2")).thenReturn(Optional.of(existing));

        Transaction result = transactionDomainService.create(
                ACCOUNT_ID, TransactionType.CREDIT, BigDecimal.TEN, "Deposit", "client-key-2", null);

        assertThat(result).isSameAs(existing);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("perna de transferência é persistida com o transferId e a chave derivada")
    void shouldPersistTransferLegWithDerivedKey() {
        UUID transferId = UUID.randomUUID();
        String key = TransactionDomainService.transferLegKey(transferId, TransactionType.DEBIT);
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        transactionDomainService.create(
                ACCOUNT_ID, TransactionType.DEBIT, BigDecimal.TEN, "Transfer to 2", key, transferId);

        ArgumentCaptor<Transaction> saved = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(saved.capture());
        assertThat(saved.getValue().getTransferId()).isEqualTo(transferId);
        assertThat(saved.getValue().getIdempotencyKey()).isEqualTo(transferId + "-DEBIT");
    }

    @Test
    @DisplayName("transferLegKey compõe transferId + nome do tipo")
    void transferLegKeyFormat() {
        UUID transferId = UUID.randomUUID();
        assertThat(TransactionDomainService.transferLegKey(transferId, TransactionType.CREDIT))
                .isEqualTo(transferId + "-CREDIT");
    }
}
