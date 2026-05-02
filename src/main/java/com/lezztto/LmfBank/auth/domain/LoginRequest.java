package com.lezztto.LmfBank.auth.domain;

public record LoginRequest(
        String username,
        String password
) {
}
