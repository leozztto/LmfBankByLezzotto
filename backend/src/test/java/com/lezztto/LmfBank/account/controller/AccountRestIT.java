package com.lezztto.LmfBank.account.controller;

import com.lezztto.LmfBank.auth.service.JwtService;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end da API de contas: criação (com mascaramento na resposta), duplicidade
 * de documento e consultas por id/documento.
 */
@AutoConfigureMockMvc
class AccountRestIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    private String bearer;

    @BeforeEach
    void authenticate() {
        bearer = "Bearer " + jwtService.generateToken("accounts-tester");
    }

    private static long accountId(String json) {
        return ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.accountId")).longValue();
    }

    private String accountBody(String documentNumber, String email) {
        return """
                {
                  "fullName": "Maria Silva",
                  "documentNumber": "%s",
                  "birthDate": "1990-05-20",
                  "motherName": "Joana Silva",
                  "nationality": "BR",
                  "email": "%s",
                  "phone": "11987654321",
                  "profession": "Engenheira",
                  "monthlyIncome": 12000.00,
                  "accountType": "C",
                  "agency": "0001",
                  "acceptedTerms": true,
                  "createdAt": "2026-01-10T09:00:00",
                  "accountStatus": "A",
                  "addresses": [
                    { "zipCode": "01001000", "street": "Praça da Sé", "neighborhood": "Sé",
                      "number": "100", "city": "São Paulo", "state": "SP", "country": "BR",
                      "addressType": "R" }
                  ]
                }
                """.formatted(documentNumber, email);
    }

    @Test
    @DisplayName("POST /accounts cria a conta, gera número, mascara dados e persiste o saldo zerado")
    void createsAccount() throws Exception {
        String response = mockMvc.perform(post("/accounts")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("12345678901", "maria.silva@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId").isNotEmpty())
                .andExpect(jsonPath("$.fullName").value("Maria Silva"))
                .andExpect(jsonPath("$.maskedDocument").value("***.456.***-01"))
                .andExpect(jsonPath("$.maskedEmail").value("ma*********@example.com"))
                .andExpect(jsonPath("$.maskedPhone").value("(11) *****-4321"))
                .andExpect(jsonPath("$.accountNumber").value(org.hamcrest.Matchers.matchesPattern("\\d{8}-\\d")))
                .andReturn().getResponse().getContentAsString();

        long id = accountId(response);
        var persisted = accountRepository.findByIdWithRelations(id).orElseThrow();
        assertThat(persisted.getBalance()).isNotNull();
        assertThat(persisted.getBalance().getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(persisted.getAddresses()).hasSize(1);
    }

    @Test
    @DisplayName("POST /accounts com documento já cadastrado retorna 409")
    void rejectsDuplicateDocument() throws Exception {
        mockMvc.perform(post("/accounts").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("99988877766", "first@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("99988877766", "second@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("GET /accounts/{id} devolve a conta quando existe e 404 quando não")
    void findById() throws Exception {
        String response = mockMvc.perform(post("/accounts").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("10101010101", "byid@example.com")))
                .andReturn().getResponse().getContentAsString();
        long id = accountId(response);

        mockMvc.perform(get("/accounts/{id}", id).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(id))
                .andExpect(jsonPath("$.maskedEmail").value("by**@example.com"));

        mockMvc.perform(get("/accounts/{id}", 999999).header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /accounts/document/{documentNumber} devolve a conta e 404 quando não existe")
    void findByDocument() throws Exception {
        mockMvc.perform(post("/accounts").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("20202020202", "bydoc@example.com")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/accounts/document/{doc}", "20202020202").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedDocument").value("***.020.***-02"));

        mockMvc.perform(get("/accounts/document/{doc}", "00000000000").header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("sem token: 401")
    void requiresAuth() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(accountBody("30303030303", "noauth@example.com")))
                .andExpect(status().isUnauthorized());
    }
}
