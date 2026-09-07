[![CI](https://github.com/leozztto/LmfBankByLezzotto/actions/workflows/ci.yml/badge.svg)](https://github.com/leozztto/LmfBankByLezzotto/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=leozztto_LmfBankByLezzotto&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=leozztto_LmfBankByLezzotto)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=leozztto_LmfBankByLezzotto&metric=coverage)](https://sonarcloud.io/summary/new_code?id=leozztto_LmfBankByLezzotto)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=leozztto_LmfBankByLezzotto&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=leozztto_LmfBankByLezzotto)

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

## Pipeline (GitHub Actions)

`.github/workflows/ci.yml` roda em cada push nas branches `main`/`develop` e em cada Pull
Request (steps executam em `backend/`; a Fase 2 divide em `backend-ci.yml` /
`frontend-ci.yml` / `e2e.yml`):

1. Build + testes unitários e de integração (`./mvnw verify`).
2. Cobertura com JaCoCo (relatórios de unidade e integração combinados).
3. Análise estática no **SonarCloud** com *Quality Gate* — o job falha se o gate reprovar
   (por padrão, cobertura de código novo abaixo de 80%).
4. Relatório de cobertura publicado como artefato do workflow.

### Cobertura do SonarCloud por evento

O plano gratuito do SonarCloud analisa **uma única branch de longa duração** (a *main branch*
do projeto) mais os **Pull Requests**. Analisar outras branches exige plano pago. Por isso:

| Evento | `verify` (build/testes/cobertura) | SonarCloud |
| --- | --- | --- |
| Pull Request (mesmo repo) | ✅ | ✅ análise de PR |
| Push na *main branch* do Sonar (`SONAR_ANALYZED_BRANCH`, padrão `main`) | ✅ | ✅ análise de branch |
| Push em qualquer outra branch (ex.: `develop`) | ✅ | ⏭️ pulado |

Como o `develop` é a branch de integração real, o ideal é torná-lo a *main branch* no
SonarCloud (*Administration → Branches*) e definir a variável `SONAR_ANALYZED_BRANCH=develop`
nas Actions — aí os merges em `develop` passam a ser analisados sem custo, e `main` fica de fora.

## Configuração do SonarCloud (uma vez)

1. Acesse <https://sonarcloud.io> e entre com a conta do GitHub.
2. **Analyze new project** → selecione `leozztto/LmfBankByLezzotto`.
3. Em *Administration → Analysis Method*, desative o *Automatic Analysis* (usamos CI).
4. Gere um token em *My Account → Security* e adicione no repositório em
   *Settings → Secrets and variables → Actions → Secrets* como **`SONAR_TOKEN`**
   (cole só o valor, sem espaços ou quebra de linha).
5. Anote a *organization key* e a *project key* reais (aparecem na URL do projeto:
   `.../organizations/<ORG>` e `?id=<PROJECT_KEY>`). Se forem diferentes dos padrões
   (`leozztto` / `leozztto_LmfBankByLezzotto`), defina-as em
   *Settings → Secrets and variables → Actions → **Variables*** como
   **`SONAR_ORG`** e **`SONAR_PROJECT_KEY`** — o workflow usa essas variáveis e não
   exige mexer no `pom.xml`.

Quando a análise do SonarCloud é esperada (PR interno ou push na `SONAR_ANALYZED_BRANCH`),
o workflow **falha de propósito** se o `SONAR_TOKEN` faltar — para um passo pulado nunca se
passar por check verde. Em PRs vindos de fork (o GitHub não expõe secrets) a análise é ignorada.

---

# Objetivo do projeto

Simular um **core banking system simplificado**, com foco em:

- consistência de dados
- resiliência a falhas
- escalabilidade horizontal
- prevenção de duplicidade de transações

---