package com.lezztto.LmfBank.account.domain.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Minimal, cross-account projection for resolving a transfer destination by account number
 * (ADR 0010) — deliberately NOT the full {@link AccountResponse}: a regular user may look up
 * anyone's account this way, so balance/address/document/status stay out of the response.
 */
@Getter
@AllArgsConstructor
public class AccountLookupResponse {

    private final Long accountId;
    private final String accountNumber;
    private final String fullName;
}
