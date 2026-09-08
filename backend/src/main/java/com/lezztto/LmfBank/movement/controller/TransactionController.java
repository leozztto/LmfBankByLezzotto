package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Transactions",
        description = "Operations related to financial transactions (credit and debit)"
)
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @Operation(
            summary = "Create a financial transaction",
            description = "Creates a CREDIT or DEBIT transaction for a given account. " +
                    "CREDIT increases balance, DEBIT decreases balance if sufficient funds exist."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Transaction created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or business rule violation"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized access"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @RequestBody @Valid TransactionRequest transactionRequest) {

        var transactionResponse = transactionService.create(transactionRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionResponse);
    }
}
