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

    public Transaction create(
            Long accountId,
            TransactionType transactionType,
            BigDecimal amount,
            String description,
            UUID transferId
    ) {

        String idempotencyKey = buildIdempotencyKey(transferId, transactionType);

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

    private String buildIdempotencyKey(UUID transferId, TransactionType transactionType) {
        return transferId + "-" + transactionType.name();
    }
}
