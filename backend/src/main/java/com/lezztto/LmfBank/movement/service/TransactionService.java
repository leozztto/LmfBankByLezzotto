package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.exception.TransactionTypeException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final DepositService depositService;
    private final WithdrawService withdrawService;
    private final TransactionMapper transactionMapper;

    public TransactionResponse create(TransactionRequest transactionRequest) {

        return transactionRepository.findByIdempotencyKey(transactionRequest.getIdempotencyKey())
                .map(transactionMapper::toResponse)
                .orElseGet(() -> process(transactionRequest));
    }

    private TransactionResponse process(TransactionRequest transactionRequest) {

        return switch (transactionRequest.getType()) {

            case CREDIT -> depositService.process(transactionRequest);

            case DEBIT -> withdrawService.process(transactionRequest);

            default -> throw new TransactionTypeException(transactionRequest.getType());
        };
    }
}
