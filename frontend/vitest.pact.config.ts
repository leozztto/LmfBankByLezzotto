import { defineConfig } from "vitest/config";
import { resolve } from "node:path";

/**
 * Config isolada dos testes de contrato (consumer Pact). Rodada só pelo script
 * `pact:test` — não entra no `npm test` nem na cobertura. Ver ADR 0008.
 *
 * - `environment: node`: os testes dirigem os route handlers do Next (server code).
 * - `pool: forks` + `singleFork`: o core nativo do Pact (`@pact-foundation/pact`)
 *   sobe um mock server por arquivo; worker threads e paralelismo dão conflito de
 *   porta e de FFI.
 */
export default defineConfig({
  test: {
    environment: "node",
    globals: false,
    include: ["pact/**/*.pact.test.ts"],
    pool: "forks",
    poolOptions: { forks: { singleFork: true } },
    // sem setupFiles do app (jsdom/MSW); cada teste declara seu próprio vi.mock.
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
      // `src/lib/env.ts` importa "server-only"; no teste vira módulo vazio.
      "server-only": resolve(__dirname, "./src/test/empty-module.ts"),
    },
  },
});
