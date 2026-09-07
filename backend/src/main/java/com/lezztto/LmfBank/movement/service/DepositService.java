package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final AccountService accountService;
    private final TransactionMapper transactionMapper;
    private final AccountValidator accountValidator;
    private final TransactionDomainService transactionDomainService;
    private final BalanceProjectionService balanceProjectionService;

    @Transactional
    public TransactionResponse process(TransactionRequest transactionRequest) {

        log.info("Transaction of type: {}", TransactionType.CREDIT.name());

        var account = accountService.findByIdAccount(transactionRequest.getAccountId());

        accountValidator.validateStatusAccountForTransaction(transactionRequest.getAccountId(), account.getAccountStatus().name());

        log.info("Generate transaction for account: {}", transactionRequest.getAccountId());

        Transaction transaction = transactionDomainService.create(
                account.getId(),
                TransactionType.CREDIT,
                transactionRequest.getAmount(),
                "Deposit",
                UUID.randomUUID()
        );

        balanceProjectionService.refresh(account.getId());

        log.info("Transaction of CREDIT completed successfully - code: {}", transaction.getId());

        return transactionMapper.toResponse(transaction);
    }
}
