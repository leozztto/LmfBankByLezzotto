package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.service.JwtService;
import com.lezztto.LmfBank.movement.domain.entity.Transaction;
import com.lezztto.LmfBank.movement.domain.enums.TransactionStatus;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import com.lezztto.LmfBank.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end do extrato: GET /accounts/statement contra o banco real.
 */
@AutoConfigureMockMvc
class BankStatementRestIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    private String bearer;
    private Long accountId;

    @BeforeEach
    void setUp() {
        // ADMIN: cobre o extrato em si, não a autorização por escopo (ADR 0010).
        bearer = "Bearer " + jwtService.generateToken("statement-tester", Role.ADMIN, null);
        accountId = persistActiveAccount();
        transactionRepository.saveAndFlush(TestData.openingCredit(accountId, new BigDecimal("200.00")));
        transactionRepository.saveAndFlush(debit(accountId, "50.00"));
    }

    private Transaction debit(Long accId, String amount) {
        return Transaction.builder()
                .id(UUID.randomUUID())
                .accountId(accId)
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal(amount))
                .status(TransactionStatus.COMPLETED)
                .description("Withdraw")
                .createdAt(LocalDateTime.now())
                .idempotencyKey("dbg-" + UUID.randomUUID())
                .build();
    }

    @Test
    @DisplayName("sem datas: extrato com todos os lançamentos e saldo atual")
    void statementWithoutDates() throws Exception {
        mockMvc.perform(get("/accounts/statement")
                        .param("accountId", accountId.toString())
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.balance").value(150.0))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.startDate").doesNotExist())
                .andExpect(jsonPath("$.endDate").doesNotExist());
    }

    @Test
    @DisplayName("com intervalo de datas: filtra pelo período e devolve as datas na resposta")
    void statementWithDateRange() throws Exception {
        String today = java.time.LocalDate.now().toString();

        mockMvc.perform(get("/accounts/statement")
                        .param("accountId", accountId.toString())
                        .param("startDate", today)
                        .param("endDate", today)
                        .header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(today))
                .andExpect(jsonPath("$.endDate").value(today))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.balance").value(150.0));
    }

    @Test
    @DisplayName("sem token: 401")
    void requiresAuth() throws Exception {
        mockMvc.perform(get("/accounts/statement").param("accountId", accountId.toString()))
                .andExpect(status().isUnauthorized());
    }
}
