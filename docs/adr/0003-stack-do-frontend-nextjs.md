# 0003 — Stack do frontend: React + Next.js 14 (App Router) + Tailwind, atrás de proxy reverso nginx

- **Status:** Aceito
- **Data:** 2026-09-06
- **Relacionado:** [0002](0002-estrutura-monorepo-e-divisao-de-modulos.md), plano de execução
  em 5 fases (artefato:
  <https://claude.ai/code/artifact/d2e4fc6c-3810-46e0-8125-7827c0d03862>)

## Contexto

O módulo `frontend/` ainda não existe — será construído do zero na Fase 3 do plano. Ele
precisa:

- ser uma aplicação web que consome a **API REST do back-end** (Spring Boot);
- autenticar por **JWT** (o back expõe `POST /auth/login` e valida `Authorization: Bearer`
  em todas as demais rotas);
- ter as telas do fluxo bancário: login, abertura de conta (formulário extenso, com lista
  de endereços), listagem/seleção de conta, saldo, depósito/saque, transferência e extrato;
- rodar em container, junto do resto da stack, para os testes E2E.

Contexto do mantenedor: perfil primariamente back-end; o projeto também tem valor de
portfólio, o que pesa a favor de tecnologias com boa empregabilidade.

### Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **Vue 3 + Vite (SPA)** | Menos boilerplate, curva suave, build estático servido direto pelo nginx (sem processo Node em runtime) | Ecossistema menor no mercado-alvo do mantenedor; sem template "baterias inclusas" equivalente |
| **React + Vite (SPA pura)** | Ecossistema React sem o peso do Next; build estático servido pelo nginx | Roteamento, data-fetching e estrutura ficam por conta do dev; menos "trilho" |
| **React + Next.js 14 (App Router) + Tailwind** | Ecossistema e empregabilidade do React; roteamento por convenção; `create-next-app` entrega template pronto com Tailwind; ótima DX; forte para portfólio | Roda como serviço Node em runtime; App Router traz SSR/RSC que uma SPA autenticada usa pouco |

## Decisão

Adotamos **React + Next.js 14 com App Router + Tailwind CSS**, partindo de um template
inicial (`create-next-app` com App Router e Tailwind).

### Comunicação entre os serviços

O front e o back conversam **pela rede de containers, na mesma origem**, com **nginx como
proxy reverso**:

```
navegador ──▶ nginx :80
               ├── /api/*  ──▶  backend:8080     (proxy_pass, prefixo /api removido)
               └── /*      ──▶  frontend:3000    (next start)
```

- O front chama `"/api/..."` — **mesma origem**, portanto **sem CORS** e sem preflight, e
  o `SecurityConfig` do back não precisa de mudança.
- O Next.js roda como **serviço Node** (`next start`) em seu próprio container — **não** é
  `output: 'export'` estático.
- O nginx também é o ponto natural para servir assets, aplicar gzip e, no futuro, terminar
  TLS.

## Consequências

- **+1 processo Node na stack** (local, no job de E2E e em produção). O `docker-compose.yaml`
  ganha um serviço `frontend` baseado em Node, mais o nginx como front controller.
- **SSR/RSC subutilizados**: por ser uma aplicação autenticada e altamente interativa, a
  maior parte das telas será *client-side* (`"use client"`). A capacidade de renderização
  no servidor do App Router fica, na prática, pouco usada — custo aceito em troca de DX,
  roteamento por convenção e template pronto.
- **Armazenamento do token JWT**: a decisão fina (cookie `httpOnly` — recomendado com Next —
  vs. memória/`localStorage`) fica para a implementação e afeta o interceptor HTTP e o
  fluxo de logout. Registrar em ADR próprio se a escolha tiver impacto de segurança
  relevante.
- **CI do frontend** (`frontend-ci.yml`): `next lint`, `next build` e testes com
  Vitest + React Testing Library, gerando `lcov` para
  `sonar.javascript.lcov.reportPaths`.
- **Papel duplo do nginx**: proxy de `/api` para o back **e** front controller do Next
  (proxy para `next start`). A config precisa lidar com o roteamento do App Router e com o
  caminho dos assets do Next.
- **Revisão futura**: se operar o serviço Next (build, memória, cold start no CI) não
  compensar, a alternativa mais leve é React + Vite como SPA estática servida direto pelo
  nginx — trocaria DX por uma stack de runtime mais simples. Seria um novo ADR substituindo
  este.
