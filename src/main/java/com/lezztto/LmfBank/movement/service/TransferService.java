package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.entity.Transfer;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.movement.mapper.TransferMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransferMapper transferMapper;

    @Transactional
    public TransferResponse createTransfer(TransferRequest transferRequest) {

        var existingTransfer = validateIdempotency(transferRequest.getIdempotencyKey());

        if (existingTransfer != null)
            return transferMapper.toTransferResponse(existingTransfer);

        validateTransfer(transferRequest);

        var fromAccount = accountService.findById(transferRequest.getFromAccountId());
        var toAccount = accountService.findById(transferRequest.getToAccountId());

        var fromBalance = fromAccount.getBalance();
        var toBalance = toAccount.getBalance();

        validateSufficientBalance(transferRequest, fromBalance.getAvailableBalance());

        Transfer transfer = Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccountId(transferRequest.getFromAccountId())
                .toAccountId(transferRequest.getToAccountId())
                .amount(transferRequest.getAmount())
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .idempotencyKey(transferRequest.getIdempotencyKey())
                .build();

        transferRepository.save(transfer);

        try {

            fromBalance.setAvailableBalance(
                    fromBalance.getAvailableBalance()
                            .subtract(transferRequest.getAmount())
            );

            toBalance.setAvailableBalance(
                    toBalance.getAvailableBalance()
                            .add(transferRequest.getAmount())
            );

            fromBalance.setTotalBalance(
                    fromBalance.getAvailableBalance()
                            .add(fromBalance.getBlockedBalance())
            );

            toBalance.setTotalBalance(
                    toBalance.getAvailableBalance()
                            .add(toBalance.getBlockedBalance())
            );

            Transaction debitTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(transferRequest.getFromAccountId())
                    .type(TransactionType.DEBIT)
                    .amount(transferRequest.getAmount())
                    .status(TransactionStatus.COMPLETED)
                    .description("Transfer to account " + transferRequest.getToAccountId())
                    .createdAt(LocalDateTime.now())
                    .transferId(transfer.getId())
                    .build();

            Transaction creditTransaction = Transaction.builder()
                    .id(UUID.randomUUID())
                    .accountId(transferRequest.getToAccountId())
                    .type(TransactionType.CREDIT)
                    .amount(transferRequest.getAmount())
                    .status(TransactionStatus.COMPLETED)
                    .description("Transfer from account " + transferRequest.getFromAccountId())
                    .createdAt(LocalDateTime.now())
                    .transferId(transfer.getId())
                    .build();

            transactionRepository.save(debitTransaction);
            transactionRepository.save(creditTransaction);

            transfer.setStatus(TransactionStatus.COMPLETED);

        } catch (Exception e) {

            transfer.setStatus(TransactionStatus.FAILED);
            transfer.setFailureReason(e.getMessage());

            throw e;
        }

        return transferMapper.toTransferResponse(transfer);
    }

    private Transfer validateIdempotency(UUID idempotencyKey) {

        return transferRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);
    }

    private void validateTransfer(TransferRequest transferRequest) {

        if (transferRequest.getFromAccountId().equals(transferRequest.getToAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }
    }

    private void validateSufficientBalance(
            TransferRequest transferRequest,
            java.math.BigDecimal availableBalance
    ) {

        if (transferRequest.getAmount().compareTo(availableBalance) > 0) {
            throw new InsufficientBalanceException(
                    transferRequest.getFromAccountId(),
                    availableBalance,
                    transferRequest.getAmount()
            );
        }
    }
}
