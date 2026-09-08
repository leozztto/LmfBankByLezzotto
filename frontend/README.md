# frontend

Web app do LmfBank — React + Next.js 14 (App Router) + Tailwind + TypeScript +
shadcn/ui + TanStack Query + React Hook Form/Zod + Zustand (ADR 0003).

```bash
npm run dev            # dev server (localhost:3000)
npm run lint           # next lint
npm run test           # Vitest + React Testing Library
npm run test:coverage  # + lcov em coverage/
npm run build          # build de produção (output standalone)
```

## Autenticação (ADR 0007)

O JWT vive num **cookie httpOnly** que o browser nunca lê. As _route handlers_ do
Next em `src/app/api/` são um **BFF**: `auth/login` chama o backend e seta o cookie;
`auth/logout` limpa; `auth/session` reporta a sessão; o catch-all `[...path]` lê o
cookie e injeta `Authorization: Bearer` ao repassar `/api/*` para `BACKEND_ORIGIN`.
O `middleware.ts` protege as rotas pela presença do cookie.

## Desenvolvimento com HMR

```bash
# na raiz: só backend + infra
docker compose up -d postgres kafka zookeeper backend

# aqui: front na :3000 com HMR
cp .env.example .env.local        # BACKEND_ORIGIN=http://localhost:8080
npm run dev
```

`fetch("/api/...")` funciona igual ao ambiente com nginx — mesma origem, sem CORS.
Em produção o nginx encaminha tudo (inclusive `/api`) para o Next; o backend não é
exposto publicamente.
