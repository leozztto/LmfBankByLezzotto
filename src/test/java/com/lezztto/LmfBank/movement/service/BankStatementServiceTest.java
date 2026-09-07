package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.response.BankStatementResponse;
import com.lezztto.LmfBank.movement.domain.response.TransactionResponse;
import com.lezztto.LmfBank.movement.mapper.TransactionMapper;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankStatementServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BalanceCalculatorService balanceCalculator;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private BankStatementService bankStatementService;

    @Test
    @DisplayName("com data inicial e final: consulta o período e devolve saldo + lançamentos mapeados")
    void withDateRange() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 1, 31);
        Transaction tx = Transaction.builder().build();
        TransactionResponse mapped = TransactionResponse.builder().build();

        when(transactionRepository.findByAccountIdAndCreatedAtBetween(
                ACCOUNT_ID, start.atStartOfDay(), end.atTime(23, 59, 59)))
                .thenReturn(List.of(tx));
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(new BigDecimal("42.00"));
        when(transactionMapper.toResponse(tx)).thenReturn(mapped);

        BankStatementResponse result = bankStatementService.getBankStatement(ACCOUNT_ID, start, end);

        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getBalance()).isEqualByComparingTo("42.00");
        assertThat(result.getStartDate()).isEqualTo(start);
        assertThat(result.getEndDate()).isEqualTo(end);
        assertThat(result.getTransactions()).containsExactly(mapped);
        verify(transactionRepository, never())
                .findByAccountIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("sem datas: usa o histórico completo ordenado por data desc")
    void withoutDates() {
        Transaction tx = Transaction.builder().build();
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(eq(ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tx)));
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionMapper.toResponse(tx)).thenReturn(TransactionResponse.builder().build());

        BankStatementResponse result = bankStatementService.getBankStatement(ACCOUNT_ID, null, null);

        assertThat(result.getTransactions()).hasSize(1);
        assertThat(result.getStartDate()).isNull();
        assertThat(result.getEndDate()).isNull();
        verify(transactionRepository, never())
                .findByAccountIdAndCreatedAtBetween(any(), any(), any());
    }

    @Test
    @DisplayName("apenas data inicial (sem final): cai no histórico completo")
    void onlyStartDateFallsBackToFullHistory() {
        when(transactionRepository.findByAccountIdOrderByCreatedAtDesc(eq(ACCOUNT_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(balanceCalculator.calculate(ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        BankStatementResponse result =
                bankStatementService.getBankStatement(ACCOUNT_ID, LocalDate.of(2026, 1, 1), null);

        assertThat(result.getTransactions()).isEmpty();
        verify(transactionRepository, never())
                .findByAccountIdAndCreatedAtBetween(any(), any(), any());
    }
}
