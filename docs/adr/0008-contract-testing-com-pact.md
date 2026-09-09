# 0008 — Contract testing com Pact (consumer no frontend, verificação no backend)

- **Status:** Aceito
- **Data:** 2026-09-09
- **Relacionado:** [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md),
  [0006](0006-ci-e-sonarcloud-por-modulo.md),
  [0007](0007-token-jwt-cookie-httponly-bff-next.md)

## Contexto

A [ADR 0002](0002-estrutura-monorepo-e-divisao-de-modulos.md) quer os módulos autônomos, cada
um com pipeline próprio. Hoje a **única** verificação do contrato entre `backend` e `frontend`
é a suíte **E2E** (`.github/workflows/e2e.yml`, Fase 4): sobe a stack inteira (PostgreSQL +
Kafka + backend + frontend + nginx) via docker-compose e roda Playwright.

Forças em jogo:

- Enquanto o E2E não roda, o `backend-ci` **sozinho não sabe** se uma mudança quebrou o que o
  front espera da API. A rede de segurança do contrato está fora dos pipelines de módulo.
- O E2E é lento (sobe 5 serviços), cross-módulo e um *required check* à parte — quanto mais
  ele cresce, mais frágil e demorado o feedback.
- O contrato que o front realmente exerce não está no client do browser (`src/lib/api/*`, que
  chama `/api/*` na mesma origem) e sim na **camada BFF** (route handlers do Next), que fala
  HTTP com `${BACKEND_ORIGIN}` injetando `Authorization: Bearer` do cookie (ADR 0007).
- Mantenedor único; a solução não pode exigir operar infra nova de forma contínua.

### Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **A. Continuar só com E2E** | Zero setup novo | Feedback lento; pipeline do backend não é independente; o E2E vira gargalo |
| **B. Contrato como arquivo versionado no repo** (front commita o pact, back verifica o arquivo) | Simples, sem serviço externo | Sem `can-i-deploy`, sem versão/branch, sem histórico de verificação; os dois lados voltam a andar acoplados no mesmo PR |
| **C. Pact com broker** — consumer no front, verificação no back, broker **PactFlow** (free tier) | O `backend-ci` sozinho já diz se uma mudança quebrou o contrato; `can-i-deploy` trava merge incompatível; E2E vira smoke | Depende de um SaaS externo e dos limites do free tier; novos secrets; tempo extra de CI; ovo-galinha no 1º PR |

## Decisão

Adotamos a opção **C**.

### Consumer (frontend)

- Testes de contrato em `frontend/pact/**/*.pact.test.ts` (`@pact-foundation/pact`, `PactV3`),
  rodados por `npm run pact:test` com `vitest.pact.config.ts` — **fora** do `npm test` e da
  cobertura.
- Cada teste **dirige a camada BFF real** (`src/app/api/**/route.ts`) com `BACKEND_ORIGIN`
  apontando para o mock server do Pact. Isso captura método, path, `Authorization: Bearer`,
  corpo da requisição e o **shape da resposta** que os schemas Zod (`src/lib/schemas/*`)
  consomem — o teste ainda valida `schema.parse(resposta)`.
- Interações cobertas: `POST /auth/login`, `POST /accounts`, `GET /accounts`,
  `GET /accounts/{id}` (200 e 404), `GET /accounts/document/{doc}`, `POST /transactions`,
  `POST /transfers`, `GET /accounts/statement`.
- IDs em path/query/corpo usam `MatchersV3.fromProviderState` — o provider injeta o valor real
  criado no `@State`.
- O pact gerado (`frontend/pact/pacts/`) é **git-ignored**; o CI publica no broker com
  `--consumer-app-version <sha>` e `--branch <branch do git>`.

### Provider (backend)

- `backend/src/test/java/com/lezztto/LmfBank/contract/BackendContractVerificationIT.java`
  (`au.com.dius.pact.provider:junit5spring`), rodado pelo **failsafe** no `mvn verify` como
  qualquer outro `*IT`.
- Verifica contra o **app real**: `@SpringBootTest(webEnvironment = RANDOM_PORT)` + PostgreSQL
  via Testcontainers, reaproveitando o container singleton de `PostgresContainerSupport`
  (mesma infra dos outros ITs — ver ADR de setup de integração / memória do projeto).
- Um filtro de requisição injeta `Authorization: Bearer <token gerado pelo JwtService>` em
  toda interação. Os `@State` semeiam dados via repositórios e limpam no `@AfterEach`.
- Roda **só quando `PACT_BROKER_BASE_URL` está no ambiente** (`@EnabledIfEnvironmentVariable`):
  `mvn verify` local e PR vindo de *fork* (sem secrets) apenas pulam a classe.
  `@IgnoreNoPactsToVerify` evita quebrar antes do primeiro pact existir.
- *Consumer version selectors*: `matchingBranch` (monorepo — os dois lados no mesmo PR) +
  `mainBranch` + `deployedOrReleased`. *Pending pacts* ligados: um contrato novo ainda não
  verificado com sucesso não reprova o build.
- Publicação do resultado da verificação ligada pelo CI via
  `-Dpact.verifier.publishResults=true -Dpact.provider.version=<sha> -Dpact.provider.branch=<branch>`.

### Broker

- **PactFlow SaaS, free tier.** Secrets no GitHub: `PACT_BROKER_BASE_URL` (pode ser *variable*)
  e `PACT_BROKER_TOKEN` (*secret*, read/write). `secrets: inherit` já os repassa aos workflows
  reutilizáveis (ADR 0006).
- Os pacticipants `lmfbank-frontend` / `lmfbank-backend` são criados no primeiro publish/verify.

### CI e gate

- `frontend-ci.yml`: passos `pact:test` → `Check Pact configuration` (fail-loud, igual ao do
  Sonar) → `Publish pacts`. PR de *fork* pula a publicação.
- `backend-ci.yml`: o `mvn verify` passa a rodar a verificação; depois, o passo
  **`can-i-deploy`** (imagem `pactfoundation/pact-cli`) compara o backend deste commit com o
  contrato mais recente do frontend na branch base e **reprova o job** se forem incompatíveis.
- **Nenhuma mudança de branch protection.** O gate roda dentro de `backend-ci`, que alimenta o
  *required check* agregado `ci` (ADR 0006). O contrato passa a ser verificado **antes** do E2E.
- `e2e.yml` continua *required check* à parte, mas rebaixado a **smoke test** — a rede de
  segurança do contrato agora é o Pact.

## Consequências

- Novos secrets e uma dependência de SaaS externo (PactFlow); os limites do free tier
  (pacticipants, retenção de versões) são suficientes para dois pacticipants.
- +~30–60s no `frontend-ci` e +tempo no `backend-ci` (verificação + `can-i-deploy`).
- **Bootstrapping:** no primeiro PR ainda não há contrato do front em `main`; o
  `can-i-deploy` pode não ter dados. É esperado destravar o gate uma única vez (ou mergear o
  contrato do front primeiro). *Pending pacts* cobrem o caso de contrato novo dentro de um PR.
- Monorepo com os dois lados mudando no mesmo PR: o provider verifica o pact da **branch do
  consumer** (`matchingBranch`), então a incompatibilidade aparece no próprio PR.
- `backend/pom.xml` ganha a dependência `au.com.dius.pact.provider:junit5spring` (test scope);
  `frontend/package.json` ganha `@pact-foundation/pact` + `@pact-foundation/pact-cli` (dev) e
  os scripts `pact:test` / `pact:publish`. O `lint` do front passa a cobrir `pact/`.
- Se a dependência do PactFlow incomodar no futuro, a saída é hospedar o `pact-broker` OSS
  (novo ADR que substitui só a seção "Broker").
- Complementa as ADRs 0002/0006; não substitui nenhuma.
