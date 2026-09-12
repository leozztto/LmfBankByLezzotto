package com.lezztto.LmfBank.auth.domain;

/**
 * Login result. {@code expiresIn} is the token lifetime in <b>seconds</b>.
 */
public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
