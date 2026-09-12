import { configDefaults, defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { resolve } from "node:path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./vitest.setup.ts",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    // Testes de contrato (Pact) rodam por conta própria — `npm run pact:test`
    // com vitest.pact.config.ts. Ver ADR 0008.
    exclude: [...configDefaults.exclude, "pact/**"],
    // Route handlers and middleware are server code — run them in node.
    // Server components (layouts/pages) are tested in jsdom with next/headers
    // and next/navigation mocked.
    environmentMatchGlobs: [
      ["src/app/api/**", "node"],
      ["src/middleware.test.ts", "node"],
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      reportsDirectory: "./coverage",
      include: ["src/**/*.{ts,tsx}"],
      // Ratchet set just below the measured numbers (lines ~99.8, branch ~98.3,
      // funcs ~99.5). The SonarCloud quality gate still owns new-code coverage;
      // this only blocks a broad local regression.
      thresholds: {
        lines: 99,
        statements: 99,
        functions: 98,
        branches: 96,
      },
      exclude: [
        "src/**/*.{test,spec}.{ts,tsx}",
        "src/test/**",
        "src/**/*.d.ts",
        // Root layout only wires next/font + globals.css; nothing to assert.
        "src/app/layout.tsx",
        // shadcn primitives are vendored verbatim — not our code to test.
        "src/components/ui/**",
      ],
    },
  },
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
      "server-only": resolve(__dirname, "./src/test/empty-module.ts"),
    },
  },
});
