package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BalanceProjectionService {

    private final BalanceCalculatorService balanceCalculatorService;
    private final AccountService accountService;

    public void refresh(Long accountId) {

        BigDecimal currentBalance =
                balanceCalculatorService.calculate(accountId);

        var account =
                accountService.findByIdAccount(accountId);

        account.getBalance().setAvailableBalance(currentBalance);
    }
}
