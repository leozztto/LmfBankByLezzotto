package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawService {

    private final AccountService accountService;
    private final TransactionMapper transactionMapper;
    private final AccountValidator accountValidator;
    private final TransactionDomainService transactionDomainService;
    private final BalanceCalculatorService balanceCalculator;
    private final BalanceProjectionService balanceProjectionService;

    @Transactional
    public TransactionResponse process(TransactionRequest transactionRequest) {

        log.info("Processing transaction of type: {}", TransactionType.DEBIT.name());

        var account = accountService.findByIdAccount(transactionRequest.getAccountId());

        accountValidator.validateStatusAccountForTransaction(transactionRequest.getAccountId(), account.getAccountStatus().name());

        var availableBalance = balanceCalculator.calculate(transactionRequest.getAccountId());

        validateSufficientBalance(transactionRequest, availableBalance);

        log.info("Generate transaction for account: {}", transactionRequest.getAccountId());

        Transaction transaction = transactionDomainService.create(
                account.getId(),
                TransactionType.DEBIT,
                transactionRequest.getAmount(),
                "Withdraw",
                UUID.randomUUID()
        );

        balanceProjectionService.refresh(account.getId());

        log.info("Transaction of DEBIT completed successfully - code: {}", transaction.getId());

        return transactionMapper.toResponse(transaction);
    }

    private void validateSufficientBalance(TransactionRequest transactionRequest, BigDecimal availableBalance) {

        if (transactionRequest.getAmount().compareTo(availableBalance) > 0) {
            throw new InsufficientBalanceException(
                    transactionRequest.getAccountId(),
                    availableBalance,
                    transactionRequest.getAmount()
            );
        }
    }
}
