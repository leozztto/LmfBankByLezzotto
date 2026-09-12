package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDomainService {

    private final TransactionRepository transactionRepository;

    /**
     * Persists a ledger entry, keyed by an explicit {@code idempotencyKey}. A second call
     * with the same key returns the existing row instead of writing a new one.
     *
     * <ul>
     *   <li>standalone deposit/withdraw: the key is the client-supplied
     *       {@code TransactionRequest.idempotencyKey}, {@code transferId} is {@code null};</li>
     *   <li>the two legs of a transfer: the key is {@code "<transferId>-DEBIT"} /
     *       {@code "<transferId>-CREDIT"}, so a retried transfer never double-posts a leg.</li>
     * </ul>
     */
    public Transaction create(
            Long accountId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            String idempotencyKey,
            UUID transferId
    ) {

        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> {

                    Transaction transaction = Transaction.builder()
                            .id(UUID.randomUUID())
                            .accountId(accountId)
                            .type(transactionType)
                            .amount(amount)
                            .status(TransactionStatus.COMPLETED)
                            .description(description)
                            .createdAt(LocalDateTime.now())
                            .transferId(transferId)
                            .idempotencyKey(idempotencyKey)
                            .build();

                    return transactionRepository.save(transaction);
                });
    }

    /** Idempotency key for a single leg of a transfer. */
    public static String transferLegKey(UUID transferId, TransactionType transactionType) {
        return transferId + "-" + transactionType.name();
    }
}
