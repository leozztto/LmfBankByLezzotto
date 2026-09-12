# 0004 — PostgreSQL como banco de dados, provisionado no docker-compose

- **Status:** Aceito
- **Data:** 2026-09-06
- **Relacionado:** [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md),
  [0005](0005-flyway-para-migracoes-de-schema.md)

## Contexto

O back-end já usa **PostgreSQL de fato** em todos os lugares:

- driver `org.postgresql:postgresql` no `pom.xml`;
- os perfis `local`, `docker` e `kubernetes` apontam para um Postgres;
- os testes de integração sobem `postgres:16-alpine` via Testcontainers;
- o `k8s/local/` tem um `Deployment` de `postgres:15`.

O que **falta**:

- O `docker-compose.yaml` da raiz **não tem** um serviço de banco. O serviço `app`, no
  perfil `docker`, aponta para `host.docker.internal:5432` — ou seja, hoje é preciso ter um
  PostgreSQL rodando na máquina *host*, manualmente, antes de `docker compose up`. Isso
  contradiz o objetivo de "subir a aplicação completa com um comando".
- As versões estão **desalinhadas**: Testcontainers usa PG 16, o `k8s/local` usa PG 15.

## Decisão

1. **PostgreSQL é o único SGBD do projeto**, em todos os ambientes. Sem banco alternativo —
   nada de H2, nem para testes (os testes de integração continuam em Postgres real via
   Testcontainers).
2. **Fixar a versão em PostgreSQL 16** (`postgres:16-alpine`) no `docker-compose.yaml`, no
   Testcontainers e no `k8s/local` — alinhar o manifesto de 15 para 16.
3. **Adicionar um serviço `postgres` ao `docker-compose.yaml` da raiz**, que sobe junto com
   `backend`, `frontend` e `nginx`:
   - imagem `postgres:16-alpine`;
   - `POSTGRES_DB=lmf_bank`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD` via variável de
     ambiente;
   - **volume nomeado** (`pgdata`) para persistência local;
   - **`healthcheck`** com `pg_isready`; o serviço `backend` declara
     `depends_on: { postgres: { condition: service_healthy } }`;
   - porta `5432` publicada no host (para inspeção com `psql` / DBeaver).
4. **Trocar `application-docker.yaml`**: host `host.docker.internal` → `postgres` (nome do
   serviço na rede do compose).
5. **Configuração por variável de ambiente** (12-factor): URL, usuário e senha vêm do
   ambiente, com *defaults* apenas para desenvolvimento local. `.env.example` versionado,
   `.env` no `.gitignore`.

## Consequências

- `docker compose up` passa a subir a aplicação inteira **sem pré-requisito** de Postgres
  no host.
- +1 serviço e +1 volume na stack (local, job de E2E, futuro ambiente *prod-like*).
- O alinhamento de versão elimina a classe de bug "passa no Testcontainers, quebra no
  compose" por diferença de comportamento entre PG 15 e 16.
- O dado local persiste entre `up`/`down` pelo volume nomeado; `docker compose down -v`
  zera — é o que o job de E2E usa para começar limpo.
- `depends_on` + `healthcheck` removem o erro/retry de boot do backend quando o banco ainda
  não aceita conexões.
- Esta ADR **não** decide como o schema é criado e versionado — isso é a
  [ADR 0005](0005-flyway-para-migracoes-de-schema.md).
