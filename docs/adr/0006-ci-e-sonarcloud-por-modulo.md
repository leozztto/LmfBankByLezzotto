# 0006 — CI e SonarCloud independentes por módulo (orquestrador + workflows reutilizáveis)

- **Status:** Aceito
- **Data:** 2026-09-07
- **Relacionado:** [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md),
  [0003](0003-stack-do-frontend-nextjs.md)

## Contexto

A [ADR 0002](0002-estrutura-monorepo-e-divisao-de-modulos.md) decidiu, no princípio, que
cada módulo é autônomo — **pipeline de CI próprio com `paths` filter, projeto SonarCloud
próprio, quality gate próprio** — e antecipou a armadilha:

> `paths` filter + branch protection: um PR que não toca `backend/` nunca reporta o check
> daquele módulo, e o merge trava esperando um status que não chega. Mitigação: um job
> *guard* (via `dorny/paths-filter`) que sempre roda e reporta, marcado como obrigatório.

Falta decidir o **como concreto**: como os workflows se organizam, qual é o *required
status check*, e como os projetos SonarCloud são nomeados e criados.

Estado de partida (fim da Fase 1): um único `.github/workflows/ci.yml` (job `build`) que
roda tudo em `backend/`, sem `paths` filter, e um único projeto SonarCloud
`leozztto_LmfBankByLezzotto` cujas coordenadas são sobrescritas em CI pelas Actions
*variables* `SONAR_ORG` / `SONAR_PROJECT_KEY`.

### Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **A. Workflows separados (`backend-ci.yml` / `frontend-ci.yml` com `on.pull_request.paths`) + um `guard.yml` à parte** | Cada módulo aparece como workflow top-level, com badge próprio | `needs` e `jobs.<id>.result` **não cruzam workflows**: o `guard.yml` não sabe se o `backend-ci.yml` passou. Para travar o merge com backend quebrado seria preciso tornar `backend-ci` *também* obrigatório — e a armadilha volta — ou usar uma action de terceiros *pollando* o status (timing frágil). |
| **B. Tudo inline num `ci.yml`** | Um arquivo só | Contraria o layout da 0002 (`.github/workflows/{backend-ci,frontend-ci,e2e}.yml`); impede rodar/`workflow_dispatch` um módulo isolado; engorda um arquivo só. |
| **C. Orquestrador `ci.yml` + `backend-ci.yml` / `frontend-ci.yml` reutilizáveis (`workflow_call`) + job-gate `ci`** | Steps de cada módulo no próprio arquivo; o job `backend`/`frontend` do orquestrador **herda o resultado agregado** do workflow chamado, então um job normal (`ci`) consegue exigir tudo — e a branch protection consegue exigir *esse* job | Job de reusable workflow não reporta status check confiável para a branch protection (por isso não pode ser o obrigatório); `secrets: inherit` repassa todos os secrets do repo. |

## Decisão

Adotamos a opção **C**.

### Workflows

- **`ci.yml` orquestra.** `on: push/pull_request` em `main` e `develop`, **sem** `paths`
  filter — sempre roda. O job `changes` usa `dorny/paths-filter@v3` e produz os outputs
  `backend` / `frontend`.
- **`backend-ci.yml` e `frontend-ci.yml` são reutilizáveis** (`on: workflow_call`, mais
  `workflow_dispatch` para rodar isolado pela UI). O `ci.yml` os chama com
  `if: needs.changes.outputs.<módulo> == 'true'` e `secrets: inherit`.
- **O job `ci`** (`needs: [changes, backend, frontend]`, `if: always()`) reprova se qualquer
  dependência terminar em `failure` ou `cancelled` e trata `skipped` (módulo não mudou) como
  OK. **`ci` é o único *required status check*** nas branches protegidas — substitui o antigo
  `build` / "Build, test & analyze".
- `e2e.yml` fica para a **Fase 4** (Playwright).

### SonarCloud

- **Dois projetos** na organização `leozztto`: **`leozztto_lmfbank-backend`** e
  **`leozztto_lmfbank-frontend`**. *Automatic Analysis* desligado (análise só via CI).
- As coordenadas passam a ser **fonte única de verdade** nos arquivos de cada módulo —
  `backend/pom.xml` (`sonar.organization` / `sonar.projectKey` / `sonar.projectName`) e
  `frontend/sonar-project.properties`. Acaba o override por Actions *variables*
  (`SONAR_ORG` / `SONAR_PROJECT_KEY` são removidas).
- **Um único secret `SONAR_TOKEN`** (um *Global Analysis Token* da organização) cobre os
  dois projetos.
- O projeto antigo `leozztto_LmfBankByLezzotto` é **deletado** depois que os dois novos
  estiverem verdes.
- O padrão do plano *free* é mantido: *branch analysis* só na "main branch" do Sonar
  (`vars.SONAR_ANALYZED_BRANCH`, default `main`); *PR analysis* em todo PR do mesmo repo;
  PR vindo de *fork* pula a análise. O workflow **falha de propósito** se `SONAR_TOKEN`
  faltar quando a análise é esperada.

## Consequências

- **A branch protection muda:** remover o *required check* `build` / "Build, test & analyze"
  e exigir `ci`. Entre o merge desta mudança e o ajuste da proteção há uma janela curta em
  que PRs abertos ficam "Expected — waiting for status"; PRs em voo precisam de *rebase*.
- Um PR que não toca um módulo **não roda o CI dele**, mas o gate `ci` fica verde — o merge
  não trava. É exatamente o objetivo da 0002.
- Dois projetos SonarCloud = a limitação de uma branch de longa duração por projeto no plano
  *free*, agora duplicada (já previsto na 0002).
- `secrets: inherit` repassa **todos** os secrets do repo aos reutilizáveis (hoje só
  `SONAR_TOKEN` e o `GITHUB_TOKEN` automático) — aceitável para um mantenedor único; se
  entrarem secrets sensíveis, trocar por passagem explícita.
- Falha de step dentro de um reusable workflow chega ao gate apenas como resultado agregado
  (`failure`), não como "qual step" — o detalhe fica no *run*.
- A cobertura do frontend hoje é ~nula (um arquivo de teste). O Quality Gate "Sonar way"
  (*Clean as You Code*) cobra só código novo, então PRs pequenos passam; a cobertura real
  vem com as telas da Fase 3.
