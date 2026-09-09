# Contract testing (Pact) — runbook

Decisão e racional completos: [ADR 0008](adr/0008-contract-testing-com-pact.md).

O `frontend` (consumer) descreve o que espera da API num **pact**; o `backend`
(provider) baixa esse pact do **broker** e verifica contra o app real. O
`backend-ci` passa a reprovar sozinho uma mudança que quebre o contrato — antes
do E2E.

```
frontend/pact/*.pact.test.ts  --pact:test-->  frontend/pact/pacts/*.json
        |                                            |
        | pact:publish (CI)                          v
        +----------------------------------->  PactFlow (broker)
                                                     |
backend BackendContractVerificationIT  <--- baixa e verifica (mvn verify, CI)
                                                     |
backend-ci: can-i-deploy  <--- reprova o PR se incompatível
```

## Setup único (feito pelo mantenedor)

1. Criar conta no **PactFlow** (free tier): <https://pactflow.io>.
2. Em *Settings → API Tokens*, copiar um token **read/write**.
3. No GitHub, em *Settings → Secrets and variables → Actions*:
   - **Variable** `PACT_BROKER_BASE_URL` = `https://<sua-org>.pactflow.io`
     (pode ser *secret* também; o CI aceita os dois).
   - **Secret** `PACT_BROKER_TOKEN` = o token do passo 2.
4. Não há mudança de *branch protection*: o gate roda dentro de `backend-ci`,
   que já alimenta o *required check* `ci` (ADR 0006).

> Enquanto os secrets não existirem, `backend-ci` e `frontend-ci` **falham de
> propósito** no passo "Check Pact configuration" (mesmo padrão do Sonar) — um
> passo pulado não pode parecer verde.

## Rodar localmente

Sem broker (só gera o pact e valida os schemas do front):

```bash
cd frontend
npm run pact:test          # -> frontend/pact/pacts/lmfbank-frontend-lmfbank-backend.json
```

```bash
cd backend
./mvnw -B verify           # BackendContractVerificationIT é PULADO (sem PACT_BROKER_BASE_URL)
```

Com um broker de teste (verificação ponta a ponta):

```bash
export PACT_BROKER_BASE_URL=https://<org>.pactflow.io
export PACT_BROKER_TOKEN=***

cd frontend && npm run pact:test && \
  npm run pact:publish -- --consumer-app-version "$(git rev-parse HEAD)" --branch "$(git rev-parse --abbrev-ref HEAD)"

cd ../backend && ./mvnw -B verify        # baixa o pact e verifica contra o app real
```

`go-task`: `task fe:pact` gera os pacts; `task be:test` cobre a verificação
quando as variáveis de ambiente estão presentes.

## O que o CI faz

- **`frontend-ci`**: `pact:test` → publica o pact no broker com a versão (SHA) e a
  branch do git. PR de *fork* não publica (sem secrets).
- **`backend-ci`**: `mvn verify` roda a verificação e (em PR do mesmo repo ou push
  em `main`) publica o resultado; depois o passo **`can-i-deploy`** reprova o job
  se o backend deste commit não for compatível com o contrato mais recente do
  front na branch base.

## Quando o `can-i-deploy` falha

Leia a tabela que a CLI imprime. Casos comuns:

| Mensagem | Significado | Ação |
|---|---|---|
| `contract published ... has not been verified` | O front mudou o contrato e o backend ainda não verificou essa versão | Rode `./mvnw verify` no PR do backend (ou espere o `backend-ci`) — se o backend precisa mudar, mude |
| verificação **falhou** | O backend não atende mais ao que o front espera | Ajuste o backend **ou** alinhe com o front que o contrato vai mudar (e atualize o `.pact.test.ts`) |
| `no versions ... found` | Bootstrapping: ainda não há contrato do front em `main` | Uma vez: destrave o gate ou mergeie o `frontend-ci` primeiro |

## Adicionar/alterar uma interação

1. Edite/adicione o `*.pact.test.ts` em `frontend/pact/` (request + `willRespondWith`
   com matchers derivados de `src/lib/schemas/*`).
2. Se usar id em path/query/corpo, use `MatchersV3.fromProviderState("${x}", exemplo)`.
3. Crie o `@State("...")` correspondente em `BackendContractVerificationIT`
   (mesmo texto do `given(...)`), semeando os dados e retornando `Map.of("x", valorReal)`.
4. `npm run pact:test` e, com broker, `./mvnw verify` para fechar o ciclo.
