package com.lezztto.LmfBank.movement.domain.response;

import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID transactionId;

    private Long accountId;

    private TransactionType type;

    private BigDecimal amount;

    private TransactionStatus status;

    private String description;

    private LocalDateTime createdAt;

    private UUID transferId;
}
