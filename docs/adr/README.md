# Architecture Decision Records (ADR)

Este diretório registra as **decisões de arquitetura** do projeto LmfBank — o *quê*, o
*porquê* e as *consequências* de cada escolha estrutural relevante.

## Formato

Usamos o formato [Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions),
enxuto:

- **Status** — `Proposto`, `Aceito`, `Substituído por NNNN`, `Descontinuado`
- **Contexto** — a situação e as forças em jogo no momento da decisão
- **Decisão** — o que foi decidido, no presente ("Adotamos…")
- **Consequências** — o que passa a ser verdade depois, incluindo os custos

## Regras

1. Arquivos são numerados sequencialmente: `NNNN-titulo-em-kebab-case.md`.
2. Um ADR aceito é **imutável**. Mudou de ideia? Crie um novo ADR que o substitui e
   atualize o `Status` do antigo para `Substituído por NNNN`.
3. Todo PR que altera a arquitetura (estrutura de módulos, build, pipeline, stack de um
   módulo, contrato entre serviços) deve **incluir ou atualizar** um ADR.

## Índice

| # | Título | Status |
|---|--------|--------|
| [0001](0001-registrar-decisoes-de-arquitetura.md) | Registrar decisões de arquitetura com ADRs | Aceito |
| [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md) | Estrutura em monorepo com módulos `backend` e `frontend` isolados | Aceito |
| [0003](0003-stack-do-frontend-nextjs.md) | Stack do frontend: React + Next.js 14 (App Router) + Tailwind, atrás de proxy reverso nginx | Aceito |
| [0004](0004-postgresql-como-banco-de-dados-no-compose.md) | PostgreSQL como banco de dados, provisionado no docker-compose | Aceito |
| [0005](0005-flyway-para-migracoes-de-schema.md) | Flyway para versionamento e migração do schema | Aceito |
| [0006](0006-ci-e-sonarcloud-por-modulo.md) | CI e SonarCloud independentes por módulo (orquestrador + workflows reutilizáveis) | Aceito |
| [0007](0007-token-jwt-cookie-httponly-bff-next.md) | Armazenamento do token JWT: cookie httpOnly + BFF no Next | Aceito |
