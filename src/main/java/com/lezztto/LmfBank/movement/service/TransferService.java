package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.idempotency.TransferIdempotencyService;
import com.lezztto.LmfBank.movement.mapper.TransferMapper;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import com.lezztto.LmfBank.movement.util.AccountValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountService accountService;
    private final TransferMapper transferMapper;
    private final AccountValidator accountValidator;
    private final TransferIdempotencyService idempotencyService;
    private final TransactionDomainService transactionDomainService;
    private final BalanceCalculatorService balanceCalculatorService;
    private final BalanceProjectionService balanceProjectionService;

    @Transactional
    public TransferResponse createTransfer(TransferRequest transferRequest) {

        log.info("Transaction of type: TRANSFER");

        if (!idempotencyService.tryLock(transferRequest.getIdempotencyKey())) {
            return transferMapper.toTransferResponse(idempotencyService.findByKey(transferRequest.getIdempotencyKey()));
        }

        validateTransfer(transferRequest);

        var fromAccount = accountService.findByIdAccount(transferRequest.getFromAccountId());
        var toAccount = accountService.findByIdAccount(transferRequest.getToAccountId());

        accountValidator.validateStatusAccountForTransaction(fromAccount.getId(), fromAccount.getAccountStatus().name());
        accountValidator.validateStatusAccountForTransaction(toAccount.getId(), toAccount.getAccountStatus().name());

        var availableBalance = balanceCalculatorService.calculate(fromAccount.getId());

        log.info("Validate balance of account: {}", fromAccount.getId());

        validateSufficientBalance(transferRequest, availableBalance);

        log.info("Generate transfer for account: {}", fromAccount.getId());

        Transfer transfer = Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amount(transferRequest.getAmount())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(transferRequest.getIdempotencyKey())
                .createdAt(LocalDateTime.now())
                .build();

        try {

            transactionDomainService.create(
                    fromAccount.getId(),
                    TransactionType.DEBIT,
                    transferRequest.getAmount(),
                    "Transfer to " + toAccount.getId(),
                    transfer.getId()
            );

            transactionDomainService.create(
                    toAccount.getId(),
                    TransactionType.CREDIT,
                    transferRequest.getAmount(),
                    "Transfer from " + fromAccount.getId(),
                    transfer.getId()
            );

            transfer.setStatus(TransactionStatus.COMPLETED);

            transferRepository.save(transfer);

            balanceProjectionService.refresh(fromAccount.getId());

            balanceProjectionService.refresh(toAccount.getId());

        } catch (Exception e) {

            transfer.setStatus(TransactionStatus.FAILED);
            transfer.setFailureReason(e.getMessage());

            transferRepository.save(transfer);

            throw e;
        }

        log.info("Transaction of TRANSFER completed successfully - code: {}", transfer.getId());

        return transferMapper.toTransferResponse(transfer);
    }

    private void validateTransfer(TransferRequest transferRequest) {

        if (transferRequest.getFromAccountId().equals(transferRequest.getToAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }
    }

    private void validateSufficientBalance(TransferRequest transferRequest, BigDecimal availableBalance) {

        if (transferRequest.getAmount().compareTo(availableBalance) > 0) {
            throw new InsufficientBalanceException(
                    transferRequest.getFromAccountId(),
                    availableBalance,
                    transferRequest.getAmount()
            );
        }
    }
}
