package com.lezztto.LmfBank.auth.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank String password
) {
}
