# 0005 — Flyway para versionamento e migração do schema

- **Status:** Aceito
- **Data:** 2026-09-06
- **Relacionado:** [0004](0004-postgresql-como-banco-de-dados-no-compose.md),
  [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md)

## Contexto

Hoje o schema do banco é gerado pelo Hibernate via
`spring.jpa.hibernate.ddl-auto: update` (perfis `local` e `docker`) e
`create-drop` (perfil `test`).

Problemas do `ddl-auto: update`:

- **Não versiona.** Não há histórico do schema, não dá para revisar uma mudança de
  estrutura em PR, nem recriar um estado específico do banco.
- **Só adiciona.** Renomear coluna, mudar tipo, remover índice ou constraint não acontece —
  o schema *deriva* silenciosamente do estado das entidades somado ao que já existia no
  banco.
- **Não é reprodutível.** A DDL gerada depende do dialeto e da versão do Hibernate; o
  resultado pode diferir entre máquinas e ao longo do tempo.
- **É perigoso perto de produção:** qualquer mudança em `@Entity` aplica DDL automática no
  *startup* da aplicação.
- **Atrapalha o E2E** (ver [ADR 0002](0002-estrutura-monorepo-e-divisao-de-modulos.md),
  fase de testes de integração): o estado inicial não é determinístico.

### Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Flyway** | SQL puro e versionado, linear, curva mínima, integração nativa com Spring Boot, checksum das migrações aplicadas | *Undo* só na edição paga — a prática é *roll forward* |
| **Liquibase** | *changelogs* em XML/YAML/JSON, abstração de dialeto, *rollback* declarativo | Mais indireção; SQL deixa de ser a fonte primária; ganho pequeno para um projeto de um banco só |

## Decisão

Adotar **Flyway** para todo o controle de schema.

- Migrações em SQL versionado em `backend/src/main/resources/db/migration/`, no padrão
  `V<versão>__<descrição>.sql` (ex.: `V1__baseline.sql`, `V2__add_transfer_status.sql`).
- Dependências: `flyway-core` + `flyway-database-postgresql` — o Flyway 10+ separou o
  suporte a PostgreSQL num módulo próprio; a versão vem gerenciada pelo *parent* do Spring
  Boot.
- **Desligar a geração automática:** `spring.jpa.hibernate.ddl-auto: validate` em todos os
  perfis de *runtime* (`local`, `docker`, `kubernetes`, `aws`). O Hibernate passa a apenas
  **validar** que as entidades batem com o schema que o Flyway criou.
- **Migração inicial `V1__baseline.sql`** descrevendo o schema atual: tabelas `account`,
  `address`, `account_balance`, `transaction`, `transfer`, `processed_event`, com chaves
  primárias e estrangeiras, os `unique` de `document_number` e `account_number`, e os
  *enums* persistidos como `varchar`. Gerar a partir de um `pg_dump --schema-only` de um
  banco criado pelo `ddl-auto` atual e **revisar à mão** antes do primeiro merge.
- **Testes de integração:** manter Testcontainers, mas trocar `ddl-auto: create-drop` por
  Flyway rodando as mesmas migrações — assim os testes validam o schema real. O
  `application-test.yaml` passa a ter `ddl-auto: validate` e Flyway habilitado.
- O Flyway roda no *startup* da aplicação (comportamento padrão do Spring Boot) e no CI
  antes dos testes de integração.

## Consequências

- Toda mudança de schema vira um **arquivo SQL revisável em PR**, com histórico linear e
  *checksum* na tabela `flyway_schema_history`.
- Fim das surpresas do `ddl-auto`: o schema é explícito e idêntico em todos os ambientes.
- **Disciplina nova:** mudar uma entidade agora exige escrever a migração correspondente.
  O `ddl-auto: validate` **falha o boot** se a migração for esquecida — o que é desejável.
- O E2E ganha estado inicial determinístico. *Seed* de dados de teste pode ser uma migração
  *repeatable* (`R__`) ativa só no perfil de teste, ou um script à parte.
- **O baseline `V1` exige cuidado:** um erro nele se propaga por todo o histórico. Revisar
  bem antes do primeiro merge.
- *Rollback*: o Flyway Community não faz `undo`. A prática adotada é *roll forward* — uma
  nova migração que corrige. Aceitável para a escala do projeto.
- Em Kubernetes com múltiplas réplicas, o *lock* do Flyway (`flyway_schema_history` +
  *advisory lock* do PostgreSQL) coordena: uma réplica migra, as outras aguardam.
