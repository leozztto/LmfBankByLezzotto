package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepositService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;

    @Transactional
    public TransactionResponse process(TransactionRequest request) {

        var account = accountService.findById(request.getAccountId());

        var accountBalance = account.getBalance();

        accountBalance.setAvailableBalance(
                accountBalance.getAvailableBalance()
                        .add(request.getAmount())
        );

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(request.getAccountId())
                .type(TransactionType.CREDIT)
                .amount(request.getAmount())
                .status(TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        var transactionEntity = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transactionEntity);
    }
}
