package com.lezztto.LmfBank.auth.domain;

import com.lezztto.LmfBank.auth.domain.enums.Role;

/** Never carries the password hash — only what's safe to echo back after creating a login. */
public record AppUserResponse(String username, Role role, Long accountId) {
}
