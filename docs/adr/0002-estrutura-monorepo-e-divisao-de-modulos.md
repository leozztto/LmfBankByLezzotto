# 0002 — Estrutura em monorepo com módulos `backend` e `frontend` isolados

- **Status:** Aceito
- **Data:** 2026-09-06
- **Relacionado:** [0003](0003-stack-do-frontend-nextjs.md), plano de execução em 5 fases
  (artefato: <https://claude.ai/code/artifact/d2e4fc6c-3810-46e0-8125-7827c0d03862>)

## Contexto

Hoje o repositório contém **um único serviço**: a API bancária em Spring Boot, projeto
Maven *single-module*, com `pom.xml`, `Dockerfile` e `docker-compose.yaml` na raiz, além de
um pipeline único em `.github/workflows/ci.yml` e um projeto no SonarCloud.

O objetivo passa a ser uma **aplicação completa** (back-end + front-end) que possa ser
executada e testada de ponta a ponta em containers, incluindo testes de integração entre os
dois lados.

Forças em jogo:

- Mantenedor único; não há times separados nem cadências de release independentes.
- O objetivo explícito é testar o fluxo inteiro junto — login, abertura de conta,
  transação, transferência, extrato.
- A comunicação entre back e front é um contrato que muda com frequência durante o
  desenvolvimento inicial.
- Já existe disciplina de CI e SonarCloud que deve ser preservada e, se possível, mantida
  independente por módulo.

### Alternativas consideradas

| Opção | Descrição | Por que não |
|---|---|---|
| **A. Repositórios separados (polyrepo)** | Um repo para o back, outro para o front | O motivo clássico do polyrepo — times, releases e controle de acesso independentes — não se aplica a um mantenedor único. Empurra o custo para exatamente onde queremos facilidade: o teste de integração entre os módulos, que passaria a exigir clonar um repo dentro do pipeline do outro e coordenar versões. |
| **B. Monorepo com módulos isolados** | Pastas `backend/` e `frontend/`, cada uma com build, container, CI e Sonar próprios | **Escolhida.** |
| **C. Monorepo Maven multi-módulo** | `pom.xml` agregador na raiz com submódulos | Maven multi-módulo serve para vários artefatos JVM. O front-end não é JVM; forçá-lo nesse modelo não traz benefício e complica o build. |

## Decisão

Adotamos um **monorepo com módulos isolados** (opção B).

### Layout

```
LmfBankByLezzotto/
├── backend/                 # o serviço Spring Boot atual, movido para cá sem virar aggregator
│   ├── pom.xml              # permanece single-module
│   ├── Dockerfile
│   └── src/
├── frontend/                # aplicação web (ver ADR 0003)
│   └── Dockerfile
├── e2e/                     # testes de integração cross-module (Playwright)
├── docker-compose.yaml      # orquestra postgres, kafka, backend, frontend
├── Taskfile.yml             # comandos padronizados entre local e CI
├── docs/adr/                # estas decisões
└── .github/workflows/
    ├── backend-ci.yml       # dispara em paths: ['backend/**']
    ├── frontend-ci.yml      # dispara em paths: ['frontend/**']
    └── e2e.yml              # dispara em mudança de qualquer módulo
```

### Princípios

- **A raiz é só orquestração** — compose, workflows, task runner, documentação. Nenhuma
  lógica de aplicação na raiz.
- **Cada módulo é autônomo** — build próprio, `Dockerfile` próprio, pipeline de CI próprio
  (com `paths` filter), projeto SonarCloud próprio, quality gate próprio.
- **O isolamento vem da estrutura, não da separação física** — pastas + filtros de path +
  CI/Sonar separados entregam a independência sem os custos de coordenação de dois repos.
- **O back-end não vira Maven multi-módulo** — apenas desce um nível de diretório.

## Consequências

### Positivas

- Uma mudança que toca o contrato entre back e front cabe em **um único PR atômico**,
  validado pelo CI como conjunto.
- Testes E2E e de integração usam **um checkout e um `docker-compose.yaml`**.
- Um só lugar para clonar, versionar e configurar.

### Custos e mitigações

- **`paths` filter + branch protection**: um PR que não toca `backend/` nunca reporta o
  check daquele módulo, e o merge trava esperando um status que não chega. Mitigação: um job
  *guard* (via `dorny/paths-filter`) que sempre roda e reporta, marcado como obrigatório.
- **Mover ~1000 arquivos para `backend/`**: histórico preservado com `git mv`; `git blame`
  precisa de `--follow` ou de um `.git-blame-ignore-revs`.
- **Dois projetos no SonarCloud**: o plano gratuito analisa uma única branch de longa
  duração por projeto — mesma limitação de hoje, agora duplicada. PR analysis continua
  cobrindo o resto.
- **CI vermelho na transição**: entre mover as pastas e ajustar os workflows, o pipeline
  antigo quebra. Mitigação: edição-ponte no `ci.yml` no mesmo PR da reestruturação.

### Reversibilidade

A decisão é reversível: `git subtree split` extrai qualquer módulo para um repositório
próprio, com histórico, caso a separação física se torne necessária no futuro.
