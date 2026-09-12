package com.lezztto.LmfBank.auth.domain;

import jakarta.validation.constraints.NotNull;

public record LinkAccountRequest(@NotNull Long accountId) {
}
