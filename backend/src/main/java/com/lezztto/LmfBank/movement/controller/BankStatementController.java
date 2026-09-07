package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.movement.domain.response.BankStatementResponse;
import com.lezztto.LmfBank.movement.service.BankStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class BankStatementController {

    private final BankStatementService bankStatementService;

    @GetMapping("/statement")
    public BankStatementResponse getStatement(
            @RequestParam Long accountId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        return bankStatementService.getBankStatement(accountId, startDate, endDate);
    }
}
