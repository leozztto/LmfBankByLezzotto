package com.lezztto.LmfBank.movement.domain.entity;

import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transaction",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_transaction_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_transaction_account_created_at", columnList = "account_id, created_at"),
                @Index(name = "idx_transaction_transfer_id", columnList = "transfer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
}
