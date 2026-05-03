package com.lezztto.LmfBank.movement.exception;

import com.lezztto.LmfBank.movement.domain.enums.TransactionType;

public class TransactionTypeException extends RuntimeException {

    private final TransactionType transactionType;

    public TransactionTypeException(TransactionType transactionType) {
        super(String.format(
                "Transaction type not supported: ", transactionType.getCode(),
                transactionType
        ));
        this.transactionType = transactionType;
    }
}
