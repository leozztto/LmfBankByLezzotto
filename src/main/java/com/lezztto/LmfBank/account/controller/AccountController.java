package com.lezztto.LmfBank.account.controller;

import com.lezztto.LmfBank.account.domain.dto.AccountDto;
import com.lezztto.LmfBank.account.domain.response.AccountResponse;
import com.lezztto.LmfBank.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Accounts",
        description = "Operations related to bank account management"
)
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

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

        return accountService.findByDocumentNumber(documentNumber);
    }
}
