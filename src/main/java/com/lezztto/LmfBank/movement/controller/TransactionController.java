package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(
            @RequestBody @Valid TransactionRequest transactionRequest) {

        var transactionResponse = transactionService.create(transactionRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionResponse);
    }
}
