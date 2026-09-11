package com.lezztto.LmfBank.auth;

import com.lezztto.LmfBank.auth.domain.enums.Role;
import com.lezztto.LmfBank.auth.service.JwtService;
import com.lezztto.LmfBank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR 0010: admin acessa qualquer conta; usuário comum só a própria (ou nenhuma, se ainda
 * não tiver sido vinculado por um admin). Tokens são forjados diretamente pelo
 * {@link JwtService} — o que importa aqui é o que o filtro/guard fazem com as claims, não o
 * fluxo de login (já coberto em {@code AuthServiceTest}/{@code AuthControllerTest}).
 */
@AutoConfigureMockMvc
class AccountAuthorizationIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    private Long ownAccountId;
    private Long otherAccountId;
    private String adminBearer;
    private String ownerBearer;
    private String strangerBearer;

    @BeforeEach
    void setUp() {
        ownAccountId = persistAccountWithBalance("300.00");
        otherAccountId = persistAccountWithBalance("300.00");

        adminBearer = "Bearer " + jwtService.generateToken("admin-test", Role.ADMIN, null);
        ownerBearer = "Bearer " + jwtService.generateToken("owner-test", Role.USER, ownAccountId);
        strangerBearer = "Bearer " + jwtService.generateToken("stranger-test", Role.USER, null);
    }

    @Test
    @DisplayName("dono vê a própria conta; não vê a conta de outro dono")
    void ownerSeesOnlyOwnAccount() throws Exception {
        mockMvc.perform(get("/accounts/{id}", ownAccountId).header("Authorization", ownerBearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}", otherAccountId).header("Authorization", ownerBearer))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("usuário sem conta vinculada: 403 em qualquer conta")
    void unlinkedUserSeesNothing() throws Exception {
        mockMvc.perform(get("/accounts/{id}", ownAccountId).header("Authorization", strangerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("id que não é seu: 403 tanto pra existente quanto inexistente — não dá pra enumerar (não vira 404)")
    void findByIdDoesNotLeakExistenceToNonOwner() throws Exception {
        long nonExistentId = otherAccountId + 999_999L;

        mockMvc.perform(get("/accounts/{id}", otherAccountId).header("Authorization", ownerBearer))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/accounts/{id}", nonExistentId).header("Authorization", ownerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("documento que não é seu: 403 tanto pra existente quanto inexistente — não dá pra enumerar CPF (não vira 404)")
    void findByDocumentDoesNotLeakExistenceToNonOwner() throws Exception {
        String otherDocument = accountRepository.findById(otherAccountId).orElseThrow().getDocumentNumber();

        mockMvc.perform(get("/accounts/document/{doc}", otherDocument).header("Authorization", ownerBearer))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/accounts/document/{doc}", "00000000000").header("Authorization", ownerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("busca por documento: dono vê o próprio; admin vê qualquer um")
    void findByDocumentWorksForOwnerAndAdmin() throws Exception {
        String ownDocument = accountRepository.findById(ownAccountId).orElseThrow().getDocumentNumber();
        String otherDocument = accountRepository.findById(otherAccountId).orElseThrow().getDocumentNumber();

        mockMvc.perform(get("/accounts/document/{doc}", ownDocument).header("Authorization", ownerBearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/document/{doc}", otherDocument).header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("admin vê qualquer conta")
    void adminSeesAnyAccount() throws Exception {
        mockMvc.perform(get("/accounts/{id}", ownAccountId).header("Authorization", adminBearer))
                .andExpect(status().isOk());
        mockMvc.perform(get("/accounts/{id}", otherAccountId).header("Authorization", adminBearer))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /accounts: dono vê só a própria (lista de 1); admin vê todas")
    void listIsScopedForOwnerAndFullForAdmin() throws Exception {
        mockMvc.perform(get("/accounts").header("Authorization", ownerBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountId").value(ownAccountId));

        mockMvc.perform(get("/accounts").header("Authorization", strangerBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/accounts").header("Authorization", adminBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("depósito: só na própria conta; na de outro dono é 403")
    void depositOnlyOnOwnAccount() throws Exception {
        mockMvc.perform(post("/transactions")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(ownAccountId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/transactions")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transactionBody(otherAccountId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("transferência: origem precisa ser própria; destino pode ser qualquer conta")
    void transferFromOwnToAnyDestination() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(ownAccountId, otherAccountId)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("transferência: debitar a conta de outro dono é 403")
    void transferFromSomeoneElsesAccountIsForbidden() throws Exception {
        mockMvc.perform(post("/transfers")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferBody(otherAccountId, ownAccountId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("extrato: só da própria conta")
    void statementOnlyForOwnAccount() throws Exception {
        mockMvc.perform(get("/accounts/statement").param("accountId", ownAccountId.toString())
                        .header("Authorization", ownerBearer))
                .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/statement").param("accountId", otherAccountId.toString())
                        .header("Authorization", ownerBearer))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("lookup por número da conta: cross-account, aberto a qualquer autenticado")
    void lookupByAccountNumberIsUnrestricted() throws Exception {
        var otherAccountNumber = accountRepository.findById(otherAccountId).orElseThrow().getAccountNumber();

        mockMvc.perform(get("/accounts/number/{accountNumber}", otherAccountNumber)
                        .header("Authorization", ownerBearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(otherAccountId))
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.balance").doesNotExist());
    }

    @Test
    @DisplayName("/admin/**: 403 pra quem não é ADMIN, mesmo autenticado")
    void adminRouteRejectsNonAdmin() throws Exception {
        mockMvc.perform(patch("/admin/users/{u}/account", "someone")
                        .header("Authorization", ownerBearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accountId\": 1}"))
                .andExpect(status().isForbidden());
    }

    private String transactionBody(Long accountId) {
        return """
                {
                  "accountId": %d,
                  "type": "C",
                  "amount": 10.00,
                  "description": "teste",
                  "idempotencyKey": "%s"
                }
                """.formatted(accountId, UUID.randomUUID());
    }

    private String transferBody(Long fromAccountId, Long toAccountId) {
        return """
                {
                  "fromAccountId": %d,
                  "toAccountId": %d,
                  "amount": 10.00,
                  "idempotencyKey": "%s"
                }
                """.formatted(fromAccountId, toAccountId, UUID.randomUUID());
    }
}
