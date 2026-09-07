package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.movement.domain.request.TransactionRequest;
import com.lezztto.LmfBank.movement.domain.request.TransferRequest;
import com.lezztto.LmfBank.movement.exception.InsufficientBalanceException;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for the debit/transfer path. They exercise the pessimistic write lock
 * added to {@code AccountRepository.findByIdForUpdate}: without it, parallel debits on the
 * same account each read a stale balance and overdraw it.
 */
class TransferConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;
    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("duas transferências simultâneas da mesma conta com saldo para uma: só uma passa")
    void onlyOneOfTwoConcurrentTransfersSucceeds() throws Exception {
        Long from = persistAccountWithBalance("100.00");
        Long dest1 = persistAccountWithBalance("0.00");
        Long dest2 = persistAccountWithBalance("0.00");

        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(2);

        for (Long dest : List.of(dest1, dest2)) {
            pool.submit(() -> {
                try {
                    startGun.await();
                    TransferRequest req = TransferRequest.builder()
                            .fromAccountId(from)
                            .toAccountId(dest)
                            .amount(new BigDecimal("80.00"))
                            .idempotencyKey(UUID.randomUUID())
                            .build();
                    transferService.createTransfer(req);
                    succeeded.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    insufficient.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertThat(finished.await(30, SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(unexpected).hasValue(0);
        assertThat(succeeded).hasValue(1);
        assertThat(insufficient).hasValue(1);
        assertThat(ledgerBalanceOf(from))
                .isEqualByComparingTo("20.00")
                .satisfies(b -> assertThat(b.signum()).isGreaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("20 saques simultâneos de 10 sobre saldo 100: exatamente 10 passam, saldo nunca fica negativo")
    void concurrentWithdrawalsNeverOverdrawTheAccount() throws Exception {
        Long accountId = persistAccountWithBalance("100.00");

        int attempts = 20;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            results.add(pool.submit(() -> {
                startGun.await();
                try {
                    transactionService.create(TransactionRequest.builder()
                            .accountId(accountId)
                            .type(TransactionType.DEBIT)
                            .amount(new BigDecimal("10.00"))
                            .description("Withdraw")
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build());
                    return true;
                } catch (InsufficientBalanceException e) {
                    return false;
                }
            }));
        }

        startGun.countDown();

        int ok = 0;
        for (Future<Boolean> r : results) {
            if (r.get(30, SECONDS)) {
                ok++;
            }
        }
        pool.shutdownNow();

        assertThat(ok).isEqualTo(10);
        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.DEBIT)).hasSize(10);
        assertThat(ledgerBalanceOf(accountId)).isEqualByComparingTo("0.00");
    }
}
