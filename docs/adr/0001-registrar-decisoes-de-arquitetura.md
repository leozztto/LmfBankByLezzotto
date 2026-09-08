# 0001 — Registrar decisões de arquitetura com ADRs

- **Status:** Aceito
- **Data:** 2026-09-06

## Contexto

O projeto está deixando de ser um serviço back-end único para se tornar uma aplicação
full-stack em monorepo (ver [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md)). As
próximas semanas concentram várias decisões estruturais — divisão de módulos, pipelines
independentes, stack do frontend, forma de comunicação entre os serviços.

O repositório é mantido por uma única pessoa. Hoje o "porquê" das escolhas vive só na
memória de quem decidiu e em descrições de PR que ninguém relê. Daqui a alguns meses a
pergunta "por que o frontend não é um repositório separado?" não terá resposta rastreável.

## Decisão

Adotamos **Architecture Decision Records** no diretório `docs/adr/`, no formato Nygard
(Status, Contexto, Decisão, Consequências), com arquivos numerados sequencialmente
(`NNNN-titulo.md`).

- Um ADR aceito é imutável; uma reversão é registrada como um **novo** ADR que substitui o
  anterior.
- PRs que mexem na arquitetura devem incluir ou atualizar um ADR.
- O `docs/adr/README.md` mantém o índice e as regras.

## Consequências

- Cada decisão estrutural passa a custar alguns parágrafos de escrita — overhead pequeno e
  pago uma vez.
- O histórico de "por que chegamos aqui" fica versionado junto do código e revisável em PR.
- Serve também de material de portfólio: mostra processo de decisão, não só o resultado.
