package com.lezztto.LmfBank.support;

import com.lezztto.LmfBank.account.domain.enums.AccountStatus;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import com.lezztto.LmfBank.movement.service.BalanceCalculatorService;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

/**
 * Base class for full-context integration tests: real Spring context, real PostgreSQL
 * (Testcontainers), Kafka listeners disabled via the {@code test} profile.
 *
 * <p>These tests are intentionally <em>not</em> {@code @Transactional}: the flows under test
 * manage their own transactions and some assertions run on separate threads, so each test
 * cleans up after itself in {@link #cleanUp()}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest extends PostgresContainerSupport {

    @Autowired
    protected AccountRepository accountRepository;
    @Autowired
    protected TransactionRepository transactionRepository;
    @Autowired
    protected TransferRepository transferRepository;
    @Autowired
    protected BalanceCalculatorService balanceCalculatorService;

    @AfterEach
    void cleanUp() {
        transferRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAll();
    }

    protected Long persistActiveAccount() {
        return accountRepository.saveAndFlush(TestData.activeAccount()).getId();
    }

    protected Long persistAccount(AccountStatus status) {
        return accountRepository.saveAndFlush(TestData.account(status)).getId();
    }

    /** Creates an active account and seeds it with an opening CREDIT of {@code amount}. */
    protected Long persistAccountWithBalance(String amount) {
        Long id = persistActiveAccount();
        transactionRepository.saveAndFlush(TestData.openingCredit(id, new BigDecimal(amount)));
        return id;
    }

    protected BigDecimal ledgerBalanceOf(Long accountId) {
        return balanceCalculatorService.calculate(accountId);
    }
}
