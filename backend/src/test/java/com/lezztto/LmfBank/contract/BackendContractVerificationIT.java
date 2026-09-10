package com.lezztto.LmfBank.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerConsumerVersionSelectors;
import au.com.dius.pact.provider.junitsupport.loader.SelectorBuilder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.lezztto.LmfBank.account.domain.entity.Account;
import com.lezztto.LmfBank.account.domain.entity.Address;
import com.lezztto.LmfBank.account.domain.enums.AddressType;
import com.lezztto.LmfBank.account.repository.AccountRepository;
import com.lezztto.LmfBank.auth.service.JwtService;
import com.lezztto.LmfBank.movement.repository.TransactionRepository;
import com.lezztto.LmfBank.movement.repository.TransferRepository;
import com.lezztto.LmfBank.support.PostgresContainerSupport;
import com.lezztto.LmfBank.support.TestData;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Verificação do provider (ADR 0008). Baixa os pacts do broker (PactFlow) e os
 * roda contra o app REAL — Spring Boot em porta aleatória + PostgreSQL via
 * Testcontainers (reaproveita o container singleton de {@link PostgresContainerSupport}).
 *
 * <p>Só executa quando {@code PACT_BROKER_BASE_URL} está no ambiente, então
 * {@code mvn verify} local e PR vindo de fork (sem secrets) apenas pulam a classe.
 *
 * <p>A publicação do resultado da verificação é ligada pelo CI via
 * {@code -Dpact.verifier.publishResults=true -Dpact.provider.version=<sha> -Dpact.provider.branch=<branch>}.
 */
@Provider("lmfbank-backend")
@PactBroker(
        url = "${PACT_BROKER_BASE_URL:http://localhost:9292}",
        authentication = @PactBrokerAuth(token = "${PACT_BROKER_TOKEN:}"),
        enablePendingPacts = "true",
        providerBranch = "${PACT_PROVIDER_BRANCH:main}"
)
@IgnoreNoPactsToVerify
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "PACT_BROKER_BASE_URL", matches = ".+")
class BackendContractVerificationIT extends PostgresContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransferRepository transferRepository;

    /**
     * Quais versões do contrato do consumer buscar: as da branch que casa com a
     * do provider (caso monorepo — os dois lados mudam no mesmo PR), a de {@code main}
     * e as marcadas como deployed/released.
     */
    @PactBrokerConsumerVersionSelectors
    public static SelectorBuilder consumerVersionSelectors() {
        return new SelectorBuilder()
                .matchingBranch()
                .mainBranch()
                .deployedOrReleased();
    }

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @AfterEach
    void cleanUp() {
        transferRepository.deleteAllInBatch();
        transactionRepository.deleteAllInBatch();
        accountRepository.deleteAll();
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verifyPact(PactVerificationContext context, HttpRequest request) {
        // O pact traz um "Authorization: Bearer <token de exemplo>" na requisição
        // (o consumer verifica que o BFF anexa um Bearer). Esse token não é
        // assinado com a chave do backend — precisamos SUBSTITUIR por um válido,
        // não só adicionar (senão o filtro lê o primeiro header e devolve 401).
        request.removeHeaders("Authorization");
        request.addHeader(
                "Authorization",
                "Bearer " + jwtService.generateToken("contract-verifier")
        );
        context.verifyInteraction();
    }

    // ---- provider states (os nomes batem com os given(...) do consumer) ----

    @State("credentials are accepted")
    void credentialsAreAccepted() {
        // /auth/login aceita qualquer usuário — nada a preparar.
    }

    @State("a new account can be created")
    void aNewAccountCanBeCreated() {
        // cleanUp() já esvaziou as tabelas; o documento do payload está livre.
    }

    @State("no account exists with a given id")
    void noAccountExists() {
        // banco vazio → GET /accounts/{id} responde 404.
    }

    @State("at least one account exists")
    void atLeastOneAccountExists() {
        persistAccountWithAddress("11111111111");
    }

    @State("an account exists")
    Map<String, Object> anAccountExists() {
        Long id = persistAccountWithAddress("22222222222");
        return Map.of("id", id);
    }

    @State("an account exists with a known document")
    Map<String, Object> anAccountExistsWithKnownDocument() {
        String document = "12345678901";
        persistAccountWithAddress(document);
        return Map.of("document", document);
    }

    @State("an active account exists")
    Map<String, Object> anActiveAccountExists() {
        Long id = accountRepository.saveAndFlush(TestData.activeAccount()).getId();
        return Map.of("accountId", id);
    }

    @State("two accounts exist for a transfer")
    Map<String, Object> twoAccountsForTransfer() {
        Long from = persistAccountWithBalance("300.00");
        Long to = accountRepository.saveAndFlush(TestData.activeAccount()).getId();
        return Map.of("fromAccountId", from, "toAccountId", to);
    }

    @State("an account exists with a completed transaction")
    Map<String, Object> anAccountWithTransaction() {
        Long id = persistAccountWithBalance("150.00");
        return Map.of("accountId", id);
    }

    // ---- helpers ----

    private Long persistAccountWithBalance(String amount) {
        Long id = accountRepository.saveAndFlush(TestData.activeAccount()).getId();
        transactionRepository.saveAndFlush(TestData.openingCredit(id, new BigDecimal(amount)));
        return id;
    }

    private Long persistAccountWithAddress(String documentNumber) {
        Account account = TestData.activeAccount();
        account.setDocumentNumber(documentNumber);
        // O contrato do front espera accountNumber no formato NNNNNNNN-N (é o que
        // o AccountService gera). TestData usa só "%08d" — normalizamos aqui.
        account.setAccountNumber(account.getAccountNumber() + "-1");
        account.addAddress(Address.builder()
                .zipCode("01001000")
                .street("Praça da Sé")
                .neighborhood("Sé")
                .number("100")
                .complement("")
                .city("São Paulo")
                .state("SP")
                .country("BR")
                .addressType(AddressType.RESIDENTIAL)
                .build());
        return accountRepository.saveAndFlush(account).getId();
    }
}
