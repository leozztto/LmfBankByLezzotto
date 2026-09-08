# frontend

Web app do LmfBank — React + Next.js 14 (App Router) + Tailwind + TypeScript (ADR 0003).

```bash
npm run dev            # dev server (localhost:3000)
npm run lint           # next lint
npm run test           # Vitest + React Testing Library
npm run test:coverage  # + lcov em coverage/
npm run build          # build de produção (output standalone)
```

Sobe junto da stack via `docker compose up` na raiz; o nginx faz o proxy de `/` para cá
e de `/api` para o backend (mesma origem).

## Desenvolvimento com HMR

Para iterar nas telas sem rebuildar o container:

```bash
docker compose up -d postgres kafka zookeeper backend   # backend + infra
cd frontend && npm run dev                              # front na :3000 com HMR
```

O `next.config.mjs` faz o proxy de `/api/*` para `http://localhost:8080` em dev (ajustável
por `BACKEND_ORIGIN`), então `fetch("/api/...")` funciona igual ao ambiente com nginx —
mesma origem, sem CORS.
