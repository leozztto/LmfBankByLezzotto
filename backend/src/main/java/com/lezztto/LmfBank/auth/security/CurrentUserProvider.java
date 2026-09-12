package com.lezztto.LmfBank.auth.security;

import com.lezztto.LmfBank.auth.domain.AuthenticatedUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the {@link AuthenticatedUser} the {@code JwtAuthenticationFilter} put in the SecurityContext. */
@Component
public class CurrentUserProvider {

    public AuthenticatedUser get() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException(
                    "No AuthenticatedUser in the SecurityContext — endpoint is missing authentication");
        }
        return user;
    }
}
