package com.lezztto.LmfBank.movement.controller;

import com.lezztto.LmfBank.auth.service.JwtService;
import com.lezztto.LmfBank.movement.domain.enums.TransactionType;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for the movement REST layer: JSON contract, JWT auth and the HTTP
 * status codes mapped from the ledger business rules.
 */
@AutoConfigureMockMvc
class MovementRestIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    private String bearer;

    @BeforeEach
    void authenticate() {
        bearer = "Bearer " + jwtService.generateToken("integration-tester");
    }

    private String creditBody(Long accountId, String amount) {
        return creditBody(accountId, amount, "Deposit", UUID.randomUUID().toString());
    }

    private String creditBody(Long accountId, String amount, String description, String idempotencyKey) {
        return """
                { "accountId": %d, "type": "C", "amount": %s, "description": "%s",
                  "idempotencyKey": "%s" }
                """.formatted(accountId, amount, description, idempotencyKey);
    }

    private String debitBody(Long accountId, String amount) {
        return """
                { "accountId": %d, "type": "D", "amount": %s, "description": "Withdraw",
                  "idempotencyKey": "%s" }
                """.formatted(accountId, amount, UUID.randomUUID());
    }

    private String transferBody(Long from, Long to, String amount) {
        return """
                { "fromAccountId": %d, "toAccountId": %d, "amount": %s, "idempotencyKey": "%s" }
                """.formatted(from, to, amount, UUID.randomUUID());
    }

    @Test
    @DisplayName("POST /transactions com token: cria CREDIT e retorna 201")
    void shouldCreateCreditTransaction() throws Exception {
        Long accountId = persistActiveAccount();

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creditBody(accountId, "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.CREDIT)).hasSize(1);
    }

    @Test
    @DisplayName("POST /transactions DEBIT sem saldo: retorna 422 INSUFFICIENT_BALANCE")
    void shouldReturn422WhenInsufficientBalance() throws Exception {
        Long accountId = persistAccountWithBalance("20.00");

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(debitBody(accountId, "50.00")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    @DisplayName("POST /transactions sem token: retorna 401")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        Long accountId = persistActiveAccount();

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creditBody(accountId, "10.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /transactions com valor inválido (0): retorna 400 VALIDATION_ERROR com fieldErrors")
    void invalidAmountIsMappedToBadRequest() throws Exception {
        Long accountId = persistActiveAccount();

        mockMvc.perform(post("/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creditBody(accountId, "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.amount").isNotEmpty());
    }

    @Test
    @DisplayName("POST /transactions usa a descrição do cliente e deduplica pela idempotencyKey")
    void usesClientDescriptionAndDeduplicates() throws Exception {
        Long accountId = persistActiveAccount();
        String key = UUID.randomUUID().toString();

        String first = mockMvc.perform(post("/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creditBody(accountId, "40.00", "aluguel", key)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("aluguel"))
                .andReturn().getResponse().getContentAsString();

        String replay = mockMvc.perform(post("/transactions")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(creditBody(accountId, "40.00", "aluguel", key)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(com.jayway.jsonpath.JsonPath.read(replay, "$.transactionId").toString())
                .isEqualTo(com.jayway.jsonpath.JsonPath.read(first, "$.transactionId").toString());
        assertThat(transactionRepository.findByAccountIdAndType(accountId, TransactionType.CREDIT)).hasSize(1);
    }

    @Test
    @DisplayName("POST /transfers: transfere entre contas e retorna 201")
    void shouldCreateTransfer() throws Exception {
        Long from = persistAccountWithBalance("300.00");
        Long to = persistAccountWithBalance("0.00");

        mockMvc.perform(post("/transfers")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(from, to, "125.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.fromAccountId").value(from))
                .andExpect(jsonPath("$.toAccountId").value(to));

        assertThat(ledgerBalanceOf(from)).isEqualByComparingTo("175.00");
        assertThat(ledgerBalanceOf(to)).isEqualByComparingTo("125.00");
    }

    @Test
    @DisplayName("POST /transfers com origem igual ao destino: retorna 400 BAD_REQUEST")
    void shouldReturn400WhenSameAccountTransfer() throws Exception {
        Long account = persistAccountWithBalance("100.00");

        mockMvc.perform(post("/transfers")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(account, account, "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
