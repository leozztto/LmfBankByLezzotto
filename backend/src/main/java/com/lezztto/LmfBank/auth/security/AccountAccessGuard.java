package com.lezztto.LmfBank.auth.security;

import com.lezztto.LmfBank.auth.exception.ForbiddenActionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Per-account authorization (ADR 0010): an admin may act on any account; a regular user
 * only on the single account linked to them ({@code AppUser.accountId}). Deliberately NOT
 * applied to a transfer's destination account — sending money to someone else's account is
 * the point of a transfer, not a scope violation (see
 * {@link com.lezztto.LmfBank.account.controller.AccountController#findByAccountNumber}).
 */
@Component
@RequiredArgsConstructor
public class AccountAccessGuard {

    private final CurrentUserProvider currentUserProvider;

    /**
     * @param accountId the resource being reached for — pass {@code null} when the caller
     *                  hasn't even resolved one (e.g. lookup by a document number/other key
     *                  that doesn't exist) so a non-admin still gets a uniform 403 instead of
     *                  the caller leaking "not found" ahead of the authorization check.
     * @throws ForbiddenActionException if the caller is not an admin and doesn't own {@code accountId}.
     */
    public void assertOwnerOrAdmin(Long accountId) {
        var user = currentUserProvider.get();
        if (user.isAdmin()) return;
        if (user.accountId() == null || !user.accountId().equals(accountId)) {
            throw new ForbiddenActionException();
        }
    }
}
