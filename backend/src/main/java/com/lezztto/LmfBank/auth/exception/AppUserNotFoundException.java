package com.lezztto.LmfBank.auth.exception;

public class AppUserNotFoundException extends RuntimeException {

    public AppUserNotFoundException(String username) {
        super(String.format("No user found with username: %s", username));
    }
}
