# Arquitetura do frontend

Referência de como o `frontend/` (Next.js 14, App Router) está organizado e por quê.
Complementa os ADRs [0003](adr/0003-stack-do-frontend-nextjs.md) (stack) e
[0007](adr/0007-token-jwt-cookie-httponly-bff-next.md) (auth). Para decisões, os ADRs
são a fonte; este documento descreve o estado atual do código.

## Visão geral

```
navegador ──► nginx (location / ) ──► Next.js (frontend:3000)
                                        │
                    ┌───────────────────┴────────────────────┐
                    │ App Router (telas, RSC + client)        │
                    │ Route handlers /api/**  (BFF)  ──────────┼──► backend:8080
                    └────────────────────────────────────────┘
```

O backend **não** é exposto publicamente pelo nginx. Todo tráfego de API do navegador
passa pelo BFF do Next, que injeta o `Authorization: Bearer` a partir do cookie httpOnly.

## Camadas

### 1. BFF — `src/app/api/**` (runtime Node)

| Rota | Papel |
|---|---|
| `api/auth/login/route.ts` | valida o corpo, chama `POST {BACKEND}/auth/login`, grava o cookie `lmf_token` httpOnly, responde `{authenticated, username, expiresAt}` — **nunca** devolve o token |
| `api/auth/logout/route.ts` | limpa o cookie (`Max-Age=0`), idempotente |
| `api/auth/session/route.ts` | `force-dynamic`; decodifica o payload do JWT (sem verificar assinatura — não há segredo no front) e devolve o estado da sessão; expirado → limpa e retorna `authenticated:false` |
| `api/[...path]/route.ts` | catch-all: repassa `GET/POST/PUT/PATCH/DELETE` para `{BACKEND}/<path>`, injeta o Bearer do cookie, não encaminha `cookie`/`host`/`authorization` do navegador; status e corpo de erro do backend passam direto |

`BACKEND_ORIGIN` vem de `src/lib/env.ts` (`import "server-only"`): `http://localhost:8080`
em `next dev`, `http://backend:8080` no compose.

### 2. Guarda de rota — `src/middleware.ts`

Edge, barato: só checa **presença** do cookie. Sem cookie + rota protegida → `307 /login?next=…`;
cookie em `/login` → `307 /dashboard`. A expiração real é responsabilidade do backend
(401 → o cliente desloga). O `matcher` exclui `api`, `_next` e arquivos estáticos.

### 3. Cliente HTTP — `src/lib/api/`

- `errors.ts` — `ApiError {status, code, message, path, fieldErrors?}`;
  `ValidationError extends ApiError` (400 + `fieldErrors`); `TransferFailedError` (422,
  transferência devolvida como `FAILED`).
- `client.ts` — `apiFetch<T>(path, options?, schema?)`. Sempre `fetch("/api/" + path)` na
  mesma origem. `!ok` com corpo objeto → `apiErrorFromBody` (vira `ValidationError` quando há
  `fieldErrors`); corpo texto → `ApiError("UPSTREAM")`. `ok` → `schema.parse(data)` se houver
  schema, senão `data` cru.
- Módulos por domínio: `auth.ts`, `accounts.ts`, `statement.ts`, `movements.ts`.
- Schemas Zod validam respostas **só onde há cálculo** (saldo do extrato, detalhe de conta,
  respostas de transação/transferência). Listagens de exibição não são revalidadas.

### 4. Estado

| Ferramenta | Uso |
|---|---|
| **TanStack Query** (`src/lib/query/`) | servidor. `makeQueryClient()`: `staleTime 30s`, sem retry em `ApiError`, `refetchOnWindowFocus:false`. `QueryCache`/`MutationCache` com `onError` → `handleGlobalError`: **401** limpa sessão + `window.location.href="/login"`; **`ValidationError`** silencioso (o form trata); resto → toast. Chaves em `query/keys.ts`. |
| **Zustand** | `auth-store` — sessão (`{authenticated, username, expiresAt}`), **não persistida**, hidratada pelo `(app)/layout.tsx` (server lê o cookie). `ui-store` — `selectedAccountId`, **persistida** em `localStorage` (`"lmf-ui"`, storage com try/catch), zerada no logout. |
| **React Hook Form + Zod** | formulários. Schema faz **só validação** (sem `.transform`, para o tipo de entrada === saída do RHF); uma função `toXPayload(values)` separada normaliza para o payload de rede. 400 `fieldErrors` → `applyFieldErrors` mapeia `addresses[0].street` → `addresses.0.street` e chama `form.setError`. |

Invalidação após mutação: criar conta → `accounts.list`; depósito/saque → `statement` +
`accounts.detail`; transferência → `statement` + `accounts.detail` das **duas** contas +
`accounts.list`. Sem optimistic update (banco: correção > latência).

### 5. Idempotência

A chave é gerada **no hook**, não na função de API, via `useRef(crypto.randomUUID())` —
o retry reusa a chave; o replay é seguro no backend. Além disso: botão
`disabled={isPending}` + `<ConfirmDialog>` antes de enviar. `resetKey()` roda quando o
usuário edita o valor, para uma nova tentativa não colidir com a anterior.

### 6. Roteamento e telas

```
(auth)/login                 → POST /auth/login
(app)/dashboard              → resumo (contagem de contas + ações rápidas)
(app)/open-account          → POST /accounts   (formulário grande + lista de endereços)
(app)/accounts              → GET /accounts  + busca por GET /accounts/document/{doc}
(app)/accounts/[id]         → GET /accounts/{id} + saldo do extrato; 404 → not-found.tsx
(app)/deposit-withdraw      → POST /transactions  (tipo "C"/"D", valor ≥ 0,01)
(app)/transfer              → POST /transfers  (422 saldo, 400 mesma conta, replay)
(app)/statement             → GET /accounts/statement?accountId&startDate&endDate
```

- `(auth)` não tem shell; `(app)` usa `<AppShell>` (sidebar + `<AccountSwitcher>` + logout +
  skip-link "Pular para o conteúdo" → `<main id="conteudo">`).
- `(app)/layout.tsx` lê o cookie no server → todas as rotas `(app)` ficam dinâmicas (esperado
  numa app autenticada).
- Fallbacks: `loading.tsx` (Skeleton) por segmento com fetch + genérico em `(app)/loading.tsx`;
  `(app)/error.tsx` e `(app)/not-found.tsx` no grupo; `app/not-found.tsx` e
  `app/global-error.tsx` na raiz.

### 7. Enums e máscaras

- `src/lib/enums.ts` — `z.enum` **específico por contexto** (o code `"C"` é ambíguo entre
  domínios) + mapas de label PT-BR + listas de `*Options` para os `<Select>`. Enums de conta
  usam code curto nos dois sentidos; enums de movimento desserializam de code mas serializam
  como NOME (request `type` `"C"/"D"`, response `type` `"CREDIT"/"DEBIT"`).
- `src/lib/masks/{cpf,phone,cep,currency}.ts` — máscaras BR. `currency` normaliza NBSP
  (` `/` `) que o `Intl.NumberFormat` insere.

## Testes

- **Vitest** + `environmentMatchGlobs` (route handlers e middleware → env `node`, resto jsdom).
- **MSW v2** (`msw/node`) — handlers casam `${BACKEND_ORIGIN}/*` **e** `/api/*`; erros por
  `server.use(...)` no teste.
- `"server-only"` é aliasado para um stub vazio no `vitest.config.ts`.
- Polyfills jsdom para Radix (`ResizeObserver`, `scrollIntoView`, `hasPointerCapture`, …) no
  `vitest.setup.ts`.
- **Radix Select/DropdownMenu não abrem em jsdom.** O `AccountSelect` reusado nos formulários
  é um `<select>` nativo de propósito (acessível e testável). O Radix Select segue no
  `account-form` (nacionalidade/tipo/endereço), testado só nos defaults.
- Cobertura: alvo "Sonar way" (código novo ≥ 80%); rodando ~97%. `layout/loading/error/
  not-found/global-error/page/providers` e `components/ui/**` saem de
  `sonar.coverage.exclusions`; **route handlers do BFF não** — são código de auth e têm teste.

## Infra

- `nginx/nginx.conf` — uma `location /` → `frontend:3000` (+ `client_max_body_size 2m`).
- `docker-compose.yaml` — `frontend` com `BACKEND_ORIGIN=http://backend:8080`,
  `NODE_ENV=production`, `COOKIE_SECURE=false` (HTTP puro atrás do nginx; ligar sob TLS).
- `next.config.mjs` — `output: "standalone"`, sem `rewrites()`.

## Limitações conhecidas

- **Sem refresh token** — a sessão morre em ~24h; o próximo fetch dá 401 → `/login`.
- **`password` ignorado no login** do backend (qualquer credencial passa) — é o estado atual;
  o front não assume rejeição de credencial.
- **Sem CSRF token** nesta fase — mitigação: `SameSite=Lax` + backend Bearer-only.
- **Navegação mobile** — a sidebar é `md:block`; abaixo de `md` não há menu (fora do escopo
  da demo).
- **`GET /accounts` sem paginação** — devolve a lista completa (mascarada).
