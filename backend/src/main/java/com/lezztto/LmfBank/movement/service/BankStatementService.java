package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.response.BankStatementResponse;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankStatementService {

    private final TransactionRepository transactionRepository;
    private final BalanceCalculatorService balanceCalculator;
    private final TransactionMapper transactionMapper;

    public BankStatementResponse getBankStatement(
            Long accountId,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<Transaction> transactions;

        if (startDate != null && endDate != null) {
            transactions = transactionRepository.findByAccountIdAndCreatedAtBetween(accountId, startDate.atStartOfDay(), endDate.atTime(23,59,59));
        } else {
            transactions = transactionRepository
                    .findByAccountIdOrderByCreatedAtDesc(accountId, Pageable.unpaged())
                    .getContent();
        }

        BigDecimal balance = balanceCalculator.calculate(accountId);

        return BankStatementResponse.builder()
                .accountId(accountId)
                .balance(balance)
                .startDate(startDate)
                .endDate(endDate)
                .transactions(
                        transactions.stream()
                                .map(transactionMapper::toResponse)
                                .toList()
                )
                .build();
    }
}
