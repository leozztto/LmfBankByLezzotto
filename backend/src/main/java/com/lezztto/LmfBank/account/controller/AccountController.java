package com.lezztto.LmfBank.account.controller;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.response.AccountLookupResponse;
import com.lezztto.LmfBank.account.domain.response.AccountResponse;
import com.lezztto.LmfBank.account.service.AccountService;
import com.lezztto.LmfBank.auth.security.AccountAccessGuard;
import com.lezztto.LmfBank.auth.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Accounts",
        description = "Operations related to bank account management"
)
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AccountAccessGuard accountAccessGuard;
    private final CurrentUserProvider currentUserProvider;

    @Operation(
            summary = "Create account",
            description = "Creates a new bank account with customer registration data"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Account created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Document number already exists",
                    content = @Content
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody @Valid AccountDto accountDto) {

        return accountService.create(accountDto);
    }

    @Operation(
            summary = "List accounts",
            description = "Admin: every registered account (no pagination — demo scope). " +
                    "Regular user: only their own linked account, if any (ADR 0010)."
    )
    @ApiResponse(responseCode = "200", description = "Accounts listed successfully")
    @GetMapping
    public List<AccountResponse> list() {
        var user = currentUserProvider.get();
        if (user.isAdmin()) {
            return accountService.findAll();
        }
        return user.accountId() == null
                ? List.of()
                : List.of(accountService.findById(user.accountId()));
    }

    @Operation(
            summary = "Find account by ID",
            description = "Returns account details by account identifier"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account found successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content
            )
    })
    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        // Checa ANTES de buscar: um id que não é seu leva 403 tanto quando existe
        // quanto quando não existe, então o status code não vira canal de
        // enumeração ("existe ou não existe essa conta?") pra quem não é dono.
        accountAccessGuard.assertOwnerOrAdmin(id);
        return accountService.findById(id);
    }

    @Operation(
            summary = "Find account by document",
            description = "Returns account details using customer document number"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Account found successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Account not found",
                    content = @Content
            )
    })
    @GetMapping("/document/{documentNumber}")
    public AccountResponse findByDocumentNumber(
            @PathVariable String documentNumber) {

        // Resolve só o id (consulta leve) e checa ANTES do fetch completo — pelo
        // mesmo motivo de findById: sem isso, um documento que existe mas não é
        // seu (403) fica distinguível de um que não existe (404), o que deixa
        // varrer CPFs cadastrados de fora da autorização.
        accountAccessGuard.assertOwnerOrAdmin(accountService.findIdByDocumentNumber(documentNumber));
        return accountService.findByDocumentNumber(documentNumber);
    }

    @Operation(
            summary = "Look up an account by number",
            description = "Minimal, cross-account projection (id, number, name) used to resolve a " +
                    "transfer destination — available to any authenticated user, not scoped to " +
                    "their own account (ADR 0010): sending money to someone else's account is the " +
                    "point of a transfer, not a scope violation."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found successfully"),
            @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    })
    @GetMapping("/number/{accountNumber}")
    public AccountLookupResponse findByAccountNumber(@PathVariable String accountNumber) {
        return accountService.findByAccountNumber(accountNumber);
    }
}
