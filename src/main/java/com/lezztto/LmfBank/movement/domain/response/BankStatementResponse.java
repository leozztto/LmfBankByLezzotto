package com.lezztto.LmfBank.movement.domain.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class BankStatementResponse {

    private Long accountId;
    private BigDecimal balance;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<TransactionResponse> transactions;
}
