package com.lezztto.LmfBank.account.exception;

public class AccountNotFoundException extends RuntimeException {

    private Long accountId = 0L;
    private String documentNumber;

    public AccountNotFoundException(Long accountId) {
        super(String.format(
                "The Account : %d not found",
                accountId
        ));
        this.accountId = accountId;
    }

    public AccountNotFoundException(String documentNumber) {
        super(String.format(
                "The Account not found for documentNumber: %s",
                documentNumber
        ));
        this.documentNumber = documentNumber;
    }

    private AccountNotFoundException(String field, String value, boolean ignored) {
        super(String.format("The Account not found for %s: %s", field, value));
    }

    public static AccountNotFoundException forAccountNumber(String accountNumber) {
        return new AccountNotFoundException("accountNumber", accountNumber, true);
    }
}
