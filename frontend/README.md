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
