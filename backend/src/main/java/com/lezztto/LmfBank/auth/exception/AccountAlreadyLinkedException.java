package com.lezztto.LmfBank.auth.exception;

public class AccountAlreadyLinkedException extends RuntimeException {

    public AccountAlreadyLinkedException(Long accountId) {
        super(String.format("Account %d is already linked to another user", accountId));
    }
}
