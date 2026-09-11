package com.lezztto.LmfBank.auth.controller;

import com.lezztto.LmfBank.auth.domain.LinkAccountRequest;
import com.lezztto.LmfBank.auth.service.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user management (ADR 0010). {@code /admin/**} is restricted to
 * {@code ROLE_ADMIN} at the filter level ({@code SecurityConfig}), so there is
 * no per-method role check here — reaching this controller already proves it.
 */
@Tag(name = "Admin", description = "Admin-only user management")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AppUserService appUserService;

    @Operation(
            summary = "Link a login to its account",
            description = "Sets the one account (ADR 0010: 1 user = 1 account) a non-admin " +
                    "login may act on. Opening an account (POST /accounts) never does this " +
                    "automatically — it's a deliberate admin action."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Linked successfully"),
            @ApiResponse(responseCode = "404", description = "User or account not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Account already linked to another user", content = @Content)
    })
    @PatchMapping("/{username}/account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkAccount(
            @PathVariable String username,
            @RequestBody @Valid LinkAccountRequest request
    ) {
        appUserService.linkAccount(username, request.accountId());
    }
}
