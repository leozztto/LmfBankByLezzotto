package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.exception.TransactionTypeException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final DepositService depositService;
    private final WithdrawService withdrawService;
    private final TransactionMapper transactionMapper;

    public TransactionResponse create(TransactionRequest transactionRequest) {

        var existingTransaction = validateIdempotency(
                transactionRequest.getIdempotencyKey()
        );

        if (existingTransaction != null) {
            return transactionMapper.toResponse(
                    existingTransaction
            );
        }

        return switch (transactionRequest.getType()) {

            case CREDIT -> depositService.process(transactionRequest);

            case DEBIT -> withdrawService.process(transactionRequest);

            default -> throw new TransactionTypeException(transactionRequest.getType());
        };
    }

    private Transaction validateIdempotency(UUID idempotencyKey) {

        return transactionRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);
    }
}
