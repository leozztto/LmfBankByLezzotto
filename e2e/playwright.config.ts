import { defineConfig, devices } from "@playwright/test";

/**
 * A stack completa é levantada por `docker compose ... --wait` (via `task e2e` ou
 * o workflow `e2e.yml`) ANTES do Playwright rodar — por isso não há `webServer`
 * aqui. `baseURL` é o nginx, o mesmo caminho que um usuário real percorre
 * (nginx → BFF Next → backend → Postgres).
 */
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost";

export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  // Estado de negócio compartilhado no Postgres: um worker só, para determinismo.
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI
    ? [["list"], ["html", { open: "never" }], ["github"]]
    : [["list"], ["html", { open: "never" }]],
  use: {
    baseURL,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
    video: "retain-on-failure",
  },
  projects: [
    { name: "chromium", use: { ...devices["Desktop Chrome"] } },
    // Firefox/WebKit ficam disponíveis para rodar local:
    //   npx playwright test --project=firefox
    { name: "firefox", use: { ...devices["Desktop Firefox"] } },
    { name: "webkit", use: { ...devices["Desktop Safari"] } },
  ],
});
