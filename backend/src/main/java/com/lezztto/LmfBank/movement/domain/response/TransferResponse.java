package com.lezztto.LmfBank.movement.domain.response;

import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private UUID transferId;

    private Long fromAccountId;

    private Long toAccountId;

    private BigDecimal amount;

    private TransactionStatus status;

    private LocalDateTime createdAt;

    private String failureReason;
}
