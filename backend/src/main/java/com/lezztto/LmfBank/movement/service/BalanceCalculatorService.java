package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceCalculatorService {

    private final TransactionRepository transactionRepository;

    public BigDecimal calculate(Long accountId) {

        BigDecimal credits = transactionRepository.sumCredits(accountId);

        BigDecimal debits = transactionRepository.sumDebits(accountId);

        return credits.subtract(debits);
    }
}
