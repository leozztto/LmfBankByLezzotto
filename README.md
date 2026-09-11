[![CI](https://github.com/leozztto/LmfBankByLezzotto/actions/workflows/ci.yml/badge.svg)](https://github.com/leozztto/LmfBankByLezzotto/actions/workflows/ci.yml)
[![Backend Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=leozztto_lmfbank-backend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=leozztto_lmfbank-backend)
[![Backend Coverage](https://sonarcloud.io/api/project_badges/measure?project=leozztto_lmfbank-backend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=leozztto_lmfbank-backend)
[![Frontend Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=leozztto_lmfbank-frontend&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=leozztto_lmfbank-frontend)
[![Frontend Coverage](https://sonarcloud.io/api/project_badges/measure?project=leozztto_lmfbank-frontend&metric=coverage)](https://sonarcloud.io/summary/new_code?id=leozztto_lmfbank-frontend)

![Java](https://img.shields.io/badge/Java-17-red?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green?logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-Event%20Streaming-black?logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Container-blue?logo=docker)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-326CE5?logo=kubernetes)
![JWT](https://img.shields.io/badge/JWT-Security-orange)

# LMF Bank API

Sistema bancário digital construído com foco em **consistência financeira, escalabilidade e segurança transacional**, utilizando arquitetura moderna baseada em **ledger + projection + event-driven design**.

---

# Estrutura (monorepo)

O repositório é um **monorepo com módulos isolados** (ADR 0002):

```
backend/    # API Spring Boot (era a raiz) — pom.xml, Dockerfile, src/, k8s/
frontend/   # Web app React + Next.js 14 (ADR 0003)
nginx/      # Proxy reverso: /api -> backend:8080 · / -> frontend:3000
docs/adr/   # Architecture Decision Records
docker-compose.yaml   # postgres · kafka · backend · frontend · nginx
Taskfile.yml          # up · down · be:test · fe:test
```

## Como rodar

```bash
cp .env.example .env          # ajuste JWT_SECRET etc.
docker compose up --build     # ou: task up

# http://localhost         -> frontend (via nginx)
# http://localhost/api/... -> backend  (via nginx, prefixo /api removido)
```

Login: **`demo` / `demo`** (usuário comum) ou **`admin` / `admin`** (vê qualquer conta —
ADR 0010), ambos semeados por migration (`V2`/`V3__app_user_roles_and_account_link.sql`).
Não há tela de cadastro de usuário de login (é distinto de abrir uma conta bancária).

`demo` começa **sem conta vinculada** — só um admin pode vincular uma (ADR 0010), nunca
automático na abertura de conta. Pra ver algo como `demo`: abra uma conta em
`/open-account` (anote o `id` na URL do resultado, `/accounts/{id}`), depois vincule via
Swagger (`http://localhost:8080/swagger-ui.html` → `POST /auth/login` como `admin` →
`Authorize` com o token → `PATCH /admin/users/demo/account`) ou por linha de comando:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}' \
  | node -pe "JSON.parse(require('fs').readFileSync(0)).token")

curl -X PATCH http://localhost:8080/admin/users/demo/account \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"accountId": <id da conta aberta>}'
```

O schema do banco é criado e versionado pelo **Flyway** (ADR 0005); o Postgres sobe no
próprio compose (ADR 0004) — não é mais preciso ter um Postgres no host.

```bash
cd backend && ./mvnw test      # só unitários (rápido, sem Docker)
cd backend && ./mvnw verify    # unitários + integração (Testcontainers + Flyway; requer Docker)
```

---

# Visão geral

O projeto simula um sistema bancário completo com suporte a:

- Criação de contas
- Depósitos (CREDIT)
- Saques (DEBIT)
- Transferências entre contas
- Consulta de saldo
- Histórico de transações

---

# Fluxos principais

## Depósito (CREDIT)

- Validação da conta
- Criação da transação
- Atualização da projeção de saldo

---

## Saque (DEBIT)

- Validação da conta
- Cálculo de saldo disponível
- Validação de saldo suficiente
- Criação da transação
- Atualização da projeção de saldo

---

## Transferência

- Validação das contas (origem e destino)
- Validação de saldo da conta origem
- Criação de duas transações:
  - DEBIT (origem)
  - CREDIT (destino)
- Registro da transferência
- Atualização dos saldos

---

# Idempotência

Para evitar duplicidade de operações:

- Cada transação possui `idempotencyKey`
- Transfers possuem controle via `TransferIdempotencyService`
- Eventos Kafka possuem controle via `ProcessedEvent`

---

# Segurança

Autenticação baseada em **JWT (JSON Web Token)**.

## Fluxo:

- Login gera token JWT
- Token enviado via header:

- Filtro valida token em cada requisição

---

# Kafka

Utilizado para processamento assíncrono de eventos de criação de conta.

## ️ Características:

- Consumer com ACK manual
- Controle de duplicidade de eventos
- Processamento idempotente

---

# Banco de dados

## Entidades principais:

- `Account`
- `AccountBalance`
- `Transaction`
- `Transfer`
- `ProcessedEvent`

---

# Tecnologias utilizadas

- Java 17+
- Spring Boot
- Spring Security
- Spring Data JPA
- Apache Kafka
- JWT
- MapStruct
- Lombok
- OpenAPI (Swagger)
- Docker
- Kubernetes
- PostgreSQL

---

# Arquitetura

- Event-driven architecture
- Ledger-based financial model
- Projection for optimized balance queries
- Idempotent operations
- Distributed processing via Kafka

---

# Qualidade e CI

## Testes

- **Unitários** (`*Test`, Surefire): regras de crédito, débito e transferência do ledger com Mockito.
- **Integração** (`*IT`, Failsafe): sobem o contexto Spring contra um **PostgreSQL real via Testcontainers**
  (slice de repositório, serviço + banco, concorrência de transferência e camada REST com JWT).

```bash
cd backend
./mvnw test      # só unitários (rápido, sem Docker)
./mvnw verify    # unitários + integração + cobertura (Flyway + Testcontainers; requer Docker)
```

## Pipeline (GitHub Actions) — CI por módulo (ADR 0006)

`.github/workflows/ci.yml` é um **orquestrador**: roda em cada push nas branches
`main`/`develop` e em cada Pull Request, **sem `paths` filter**. O job `changes`
(`dorny/paths-filter@v3`) decide o que roda; `backend-ci.yml` e `frontend-ci.yml` são
**workflows reutilizáveis** (`workflow_call`) chamados só quando o módulo muda. O job
**`ci`** agrega os resultados e é o **único *required status check*** — reprova se um módulo
necessário falhar/cancelar; passa se o módulo foi pulado. (`e2e.yml` fica para a Fase 4.)

| PR toca… | backend-ci | frontend-ci | job `ci` |
| --- | --- | --- | --- |
| só `backend/**` | ✅ | ⏭️ skip | ✅ reporta |
| só `frontend/**` | ⏭️ skip | ✅ | ✅ reporta |
| ambos | ✅ | ✅ | ✅ reporta |
| só raiz / `docs/` | ⏭️ skip | ⏭️ skip | ✅ verde |

- **backend-ci**: `./mvnw -B verify` (unitários + integração Testcontainers, Flyway aplica as
  migrações) → cobertura JaCoCo (unidade + integração) → análise **SonarCloud** com *Quality
  Gate* → relatório de cobertura como artefato.
- **frontend-ci**: `npm ci` → `next lint` → Vitest + React Testing Library (`lcov`) →
  `next build` → análise **SonarCloud** (`sonarqube-scan-action`) com *Quality Gate*.

### Cobertura do SonarCloud por evento

O plano gratuito analisa **uma única branch de longa duração por projeto** (a *main branch*)
mais os **Pull Requests**. Por isso, para cada projeto:

| Evento | build/testes/cobertura | SonarCloud |
| --- | --- | --- |
| Pull Request (mesmo repo) | ✅ | ✅ análise de PR |
| Push na *main branch* do Sonar (`SONAR_ANALYZED_BRANCH`, padrão `main`) | ✅ | ✅ análise de branch |
| Push em qualquer outra branch (ex.: `develop`) | ✅ | ⏭️ pulado |
| Pull Request vindo de *fork* | ✅ | ⏭️ pulado (GitHub não expõe secrets) |

## Configuração do SonarCloud (uma vez)

**Dois projetos** na organização `leozztto`, um por módulo:

| Módulo | Project key | Coordenadas em |
| --- | --- | --- |
| backend | `leozztto_lmfbank-backend` | `backend/pom.xml` (`<sonar.*>`) |
| frontend | `leozztto_lmfbank-frontend` | `frontend/sonar-project.properties` |

1. Em <https://sonarcloud.io>, org `leozztto` → criar os dois projetos com **exatamente**
   essas keys.
2. Em cada um, *Administration → Analysis Method*: desativar o *Automatic Analysis* (usamos CI).
3. *Administration → Branches*: definir a *main branch* de cada projeto como `main` (ou
   `develop`, e então `vars.SONAR_ANALYZED_BRANCH=develop` nas Actions).
4. Associar o *Quality Gate* "Sonar way" (*Clean as You Code* — só código novo).
5. Gerar um **Global Analysis Token** em *My Account → Security* e adicioná-lo em
   *Settings → Secrets and variables → Actions → Secrets* como **`SONAR_TOKEN`** — um único
   token cobre os dois projetos.

Quando a análise é esperada (PR interno ou push na `SONAR_ANALYZED_BRANCH`), o workflow
**falha de propósito** se o `SONAR_TOKEN` faltar — para um passo pulado nunca passar por
check verde.

---

# Objetivo do projeto

Simular um **core banking system simplificado**, com foco em:

- consistência de dados
- resiliência a falhas
- escalabilidade horizontal
- prevenção de duplicidade de transações

---