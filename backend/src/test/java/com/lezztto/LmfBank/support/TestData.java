package com.lezztto.LmfBank.support;

import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.entity.AccountBalance;
import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.domain.enums.AccountType;
import com.lezztto.LmfBank.account.domain.enums.Nationality;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Factory helpers for building valid domain entities in integration tests.
 */
public final class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private TestData() {
    }

    public static Account activeAccount() {
        return account(AccountStatus.ACTIVE);
    }

    public static Account account(AccountStatus status) {
        int n = SEQ.incrementAndGet();
        Account account = Account.builder()
                .fullName("Account Holder " + n)
                .documentNumber(String.format("%011d", n))
                .birthDate(LocalDate.of(1990, 1, 1))
                .motherName("Mother " + n)
                .nationality(Nationality.BRAZILIAN)
                .email("holder" + n + "@example.com")
                .phone("+55 11 90000-000" + (n % 10))
                .profession("Engineer")
                .monthlyIncome(new BigDecimal("10000.00"))
                .accountType(AccountType.CHECKING)
                .accountNumber(String.format("%08d", n))
                .agency("0001")
                .acceptedTerms(true)
                .createdAt(LocalDateTime.now())
                .accountStatus(status)
                .build();

        account.addBalance(AccountBalance.builder()
                .availableBalance(BigDecimal.ZERO)
                .blockedBalance(BigDecimal.ZERO)
                .totalBalance(BigDecimal.ZERO)
                .build());

        return account;
    }

    /**
     * A completed CREDIT transaction, used to give an account an opening balance without
     * going through the deposit flow.
     */
    public static Transaction openingCredit(Long accountId, BigDecimal amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accountId)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .status(TransactionStatus.COMPLETED)
                .description("Opening balance")
                .createdAt(LocalDateTime.now())
                .transferId(null)
                .idempotencyKey("opening-" + UUID.randomUUID())
                .build();
    }
}
