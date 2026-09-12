package com.lezztto.LmfBank.movement.service;

import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Regra do saldo do ledger: saldo disponível = soma dos CRÉDITOS - soma dos DÉBITOS.
 */
@ExtendWith(MockitoExtension.class)
class BalanceCalculatorServiceTest {

    private static final Long ACCOUNT_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BalanceCalculatorService balanceCalculatorService;

    @Test
    @DisplayName("saldo = créditos - débitos")
    void shouldReturnCreditsMinusDebits() {
        when(transactionRepository.sumCredits(ACCOUNT_ID)).thenReturn(new BigDecimal("300.00"));
        when(transactionRepository.sumDebits(ACCOUNT_ID)).thenReturn(new BigDecimal("125.50"));

        assertThat(balanceCalculatorService.calculate(ACCOUNT_ID)).isEqualByComparingTo("174.50");
    }

    @Test
    @DisplayName("conta sem movimentação: saldo zero")
    void shouldReturnZeroWhenNoTransactions() {
        when(transactionRepository.sumCredits(ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumDebits(ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        assertThat(balanceCalculatorService.calculate(ACCOUNT_ID)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("débitos maiores que créditos: saldo negativo")
    void shouldReturnNegativeWhenDebitsExceedCredits() {
        when(transactionRepository.sumCredits(ACCOUNT_ID)).thenReturn(new BigDecimal("40.00"));
        when(transactionRepository.sumDebits(ACCOUNT_ID)).thenReturn(new BigDecimal("100.00"));

        assertThat(balanceCalculatorService.calculate(ACCOUNT_ID)).isEqualByComparingTo("-60.00");
    }

    @Test
    @DisplayName("apenas créditos: saldo igual à soma dos créditos")
    void shouldReturnCreditsWhenNoDebits() {
        when(transactionRepository.sumCredits(ACCOUNT_ID)).thenReturn(new BigDecimal("99.99"));
        when(transactionRepository.sumDebits(ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        assertThat(balanceCalculatorService.calculate(ACCOUNT_ID)).isEqualByComparingTo("99.99");
    }
}
