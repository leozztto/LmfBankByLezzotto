package com.lezztto.LmfBank.auth.domain;

import com.lezztto.LmfBank.auth.domain.enums.Role;

/**
 * The JWT's claims, decoded — set as the {@code Authentication} principal by
 * {@link com.lezztto.LmfBank.auth.filter.JwtAuthenticationFilter} for every
 * authenticated request. {@code accountId} is {@code null} for a user not yet
 * linked to an account (ADR 0010) — everything account-scoped must treat that
 * as "no account to see", not as "sees everything".
 */
public record AuthenticatedUser(String username, Role role, Long accountId) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
