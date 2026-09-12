package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.auth.security.AccountAccessGuard;
import com.lezztto.LmfBank.movement.domain.response.TransferResponse;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Transfers",
        description = "Transfer operations between accounts"
)
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final AccountAccessGuard accountAccessGuard;

    @Operation(
            summary = "Create transfer",
            description = "Transfers amount from one account to another"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer completed successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Duplicate idempotency key"
            )
    })
    @PostMapping
    public ResponseEntity<TransferResponse> create(@RequestBody @Valid TransferRequest transferRequest) {

        // Só a origem é dona do dinheiro que sai; o destino pode ser qualquer conta —
        // é o próprio propósito de uma transferência (ADR 0010).
        accountAccessGuard.assertOwnerOrAdmin(transferRequest.getFromAccountId());

        var transferResponse = transferService.createTransfer(transferRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(transferResponse);
    }
}
