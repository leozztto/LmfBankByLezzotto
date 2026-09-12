# 0007 — Armazenamento do token JWT: cookie httpOnly + BFF no Next

- **Status:** Aceito
- **Data:** 2026-09-08
- **Relacionado:** [0003](0003-stack-do-frontend-nextjs.md), [0006](0006-ci-e-sonarcloud-por-modulo.md)

## Contexto

A [ADR 0003](0003-stack-do-frontend-nextjs.md) adotou Next.js atrás do nginx e deixou
**explicitamente em aberto** onde guardar o JWT:

> **Armazenamento do token JWT**: a decisão fina (cookie `httpOnly` — recomendado com Next —
> vs. memória/`localStorage`) fica para a implementação e afeta o interceptor HTTP e o fluxo
> de logout. Registrar em ADR próprio se a escolha tiver impacto de segurança relevante.

A Fase 3 começa a construir as telas e precisa dessa decisão. Forças em jogo:

- A aplicação é autenticada e altamente client-side (`"use client"`).
- O backend é **stateless** (HS256, só entende `Authorization: Bearer`), **sem** refresh
  token, **sem** `/auth/me`, **sem** endpoint de logout. Após a Fase 3.0, `POST /auth/login`
  responde `{ token, tokenType, expiresIn }`.
- `localStorage`/memória expõem o token a **XSS** e tornam o logout e o "lembrar sessão"
  frágeis.
- O contrato "mesma origem" via nginx (ADR 0003) deve ser preservado — nada de CORS.
- Mantenedor único; simplicidade importa.

### Alternativas consideradas

| Opção | Prós | Contras |
|---|---|---|
| **A. SPA + token em memória/`localStorage`** + header no cliente | Simples; backend intocado | Token exposto a XSS; some no reload (memória) ou fica persistido em texto (`localStorage`); guarda de rota só client-side |
| **B. Cookie httpOnly setado pelo backend** | Token fora do JS | Acopla o backend a `Set-Cookie`, CSRF e ao ciclo de vida do cookie; muda o `SecurityConfig` |
| **C. Cookie httpOnly + BFF no Next** (route handlers) | Token fora do JS; backend permanece stateless e sem saber de cookies; um único lugar para injetar o `Bearer` e tratar 401; mesma origem mantida | Todo o tráfego de API passa pelo processo Node; o cliente não consegue ler o cookie (precisa de outro caminho para saber se está logado) |

## Decisão

Adotamos a opção **C**.

- Cookie **`lmf_token`** — `HttpOnly; SameSite=Lax; Path=/`; `Secure` apenas quando
  `COOKIE_SECURE=true` (sob TLS). `Max-Age` = `expiresIn` do login (segundos); _fallback_
  para o `exp` do próprio JWT.
- **Route handlers** em `frontend/src/app/api/`:
  - `auth/login` — valida o corpo, chama `${BACKEND_ORIGIN}/auth/login`, seta o cookie e
    responde `{ authenticated, username, expiresAt }` — **nunca** devolve o token ao browser;
  - `auth/logout` — limpa o cookie (`Max-Age=0`);
  - `auth/session` — decodifica o _payload_ do JWT (sem verificar assinatura — o frontend
    não tem o segredo) e reporta a sessão;
  - `[...path]` — catch-all: lê o cookie, injeta `Authorization: Bearer` e repassa todo o
    resto de `/api/*` para `BACKEND_ORIGIN`. **Não** encaminha `cookie`/`host`/`connection`
    nem qualquer `authorization` vindo do browser. Status e corpo (`ApiError`) do backend
    passam direto; 401 passa direto e o cliente desloga.
- **nginx passa a rotear `/api` para o Next** (não mais direto para o backend). O backend
  deixa de ser exposto publicamente pelo nginx (continua publicado em `:8080` no compose
  para dev — Swagger, debug). O `rewrites()` de `/api` do `next.config.mjs` é **removido**
  (o catch-all o substitui em todos os ambientes).
- `middleware.ts` protege as rotas pela **presença** do cookie (barato, edge). A expiração
  real é enforçada pelo 401 do backend → o cliente limpa a sessão e vai para `/login`.
- **Sem `GET /accounts/me`**: o login não tem _user store_ (`sub` é um username arbitrário,
  qualquer senha passa) — não há vínculo usuário↔conta a expor.
- **Sem token CSRF** nesta fase. Mitigação: `SameSite=Lax` + o backend só aceita `Bearer`
  (ignora cookies), então CSRF clássico não tem efeito.

## Consequências

- nginx fica com uma única `location /`; o processo Node vira **caminho crítico** de toda a
  API.
- Como o cookie é httpOnly, a UI descobre a sessão por três caminhos: (1) `(app)/layout.tsx`
  (server) lê o cookie e injeta `initialSession`; (2) `GET /api/auth/session` revalida sem
  reload; (3) `middleware.ts` (o portão de fato). O _store_ de sessão (Zustand) é só UX.
  Rejeitado: um cookie-_hint_ não-httpOnly (superfície extra, risco de dessincronizar).
- `Max-Age` do cookie = 24h, **sem refresh** → quando expira, o próximo `fetch` recebe 401 e
  leva para `/login`.
- O frontend **não** recebe o `JWT_SECRET` — a verificação criptográfica é sempre do backend.
- `docker-compose.yaml` ganha `BACKEND_ORIGIN` / `NODE_ENV` / `COOKIE_SECURE` no serviço
  `frontend`.
- As route handlers (`src/app/**/route.ts`) **saem** de `sonar.coverage.exclusions` — são
  código sensível de auth e passam a ser cobertas por testes.
- Complementa a [ADR 0003](0003-stack-do-frontend-nextjs.md), não a substitui.
