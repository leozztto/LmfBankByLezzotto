package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionMapper transactionMapper;
    private final AccountValidator accountValidator;

    @Transactional
    public TransactionResponse process(TransactionRequest transactionRequest) {

        var account = accountService.findById(transactionRequest.getAccountId());
        var accountBalance = account.getBalance();

        accountValidator.validateForTransaction(account.getId(), account.getAccountStatus().name());

        if (accountBalance.getAvailableBalance()
                .compareTo(transactionRequest.getAmount()) < 0) {

            throw new InsufficientBalanceException(
                    transactionRequest.getAccountId(),
                    accountBalance.getAvailableBalance(),
                    transactionRequest.getAmount()
            );
        }

        accountBalance.setAvailableBalance(
                accountBalance.getAvailableBalance()
                        .subtract(transactionRequest.getAmount())
        );

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(transactionRequest.getAccountId())
                .type(TransactionType.DEBIT)
                .amount(transactionRequest.getAmount())
                .status(TransactionStatus.COMPLETED)
                .description(transactionRequest.getDescription())
                .createdAt(LocalDateTime.now())
                .idempotencyKey(transactionRequest.getIdempotencyKey())
                .build();

        var transactionEntity = transactionRepository.save(transaction);

        return transactionMapper.toResponse(transactionEntity);
    }
}
