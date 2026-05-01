package com.lezztto.LmfBank.movement.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    private final Long accountId;
    private final BigDecimal currentBalance;
    private final BigDecimal requestedAmount;

    public InsufficientBalanceException(Long accountId,
                                        BigDecimal currentBalance,
                                        BigDecimal requestedAmount) {
        super(String.format(
                "Saldo insuficiente para conta %d. Saldo atual: %s, valor solicitado: %s",
                accountId, currentBalance, requestedAmount
        ));
        this.accountId = accountId;
        this.currentBalance = currentBalance;
        this.requestedAmount = requestedAmount;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }
}
