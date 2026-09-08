import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { resolve } from "node:path";

export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: "./vitest.setup.ts",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    // Route handlers and middleware are server code — run them in node.
    environmentMatchGlobs: [
      ["src/app/api/**", "node"],
      ["src/middleware.test.ts", "node"],
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "lcov"],
      reportsDirectory: "./coverage",
      include: ["src/**/*.{ts,tsx}"],
      // Ratchet set just below the measured numbers (lines ~95.7, branch ~88).
      // The SonarCloud quality gate still owns new-code coverage; this only
      // blocks a broad local regression.
      thresholds: {
        lines: 94,
        statements: 94,
        functions: 92,
        branches: 85,
      },
      exclude: [
        "src/**/*.{test,spec}.{ts,tsx}",
        "src/test/**",
        "src/**/*.d.ts",
        "src/app/**/{layout,loading,error,not-found,template}.tsx",
        "src/app/**/page.tsx",
        "src/app/providers.tsx",
        "src/components/ui/**",
        "src/components/app-shell.tsx",
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
