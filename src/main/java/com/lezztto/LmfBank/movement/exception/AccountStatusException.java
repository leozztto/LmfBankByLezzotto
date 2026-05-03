package com.lezztto.LmfBank.movement.exception;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;

public class AccountStatusException extends RuntimeException {

    private Long accountID = 0L;
    private String accountStatus = "";

    public AccountStatusException(Long accountID, String accountStatus) {
        super(String.format(
                "Account: %d is not allowed for transactions. Status: %s",
                accountID, accountStatus
        ));
        this.accountID = accountID;
        this.accountStatus = accountStatus;
    }
}
