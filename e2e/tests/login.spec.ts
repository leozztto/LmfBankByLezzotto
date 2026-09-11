import { test, expect } from "@playwright/test";

import { loginViaUi } from "../fixtures/flows";

test.describe("login", () => {
  test("credenciais válidas → token e redireciona para o dashboard", async ({
    page,
    context,
  }) => {
    await loginViaUi(page);

    // dashboard renderizado (o AppShell só monta com sessão válida)
    await expect(page.getByRole("heading", { name: "Painel" })).toBeVisible();
    await expect(page.getByRole("link", { name: "Contas" })).toBeVisible();

    // o BFF gravou o cookie httpOnly de sessão
    const cookies = await context.cookies();
    expect(cookies.map((c) => c.name)).toContain("lmf_token");
    expect(cookies.find((c) => c.name === "lmf_token")?.httpOnly).toBe(true);
  });

  test("uma rota protegida sem sessão joga para /login", async ({ page }) => {
    await page.goto("/accounts");
    await expect(page).toHaveURL(/\/login(\?|$)/);
  });

  test("credenciais inválidas → mensagem de erro, sem sessão (ADR 0009)", async ({
    page,
    context,
  }) => {
    await page.goto("/login");
    await page.getByLabel("Usuário").fill("1");
    await page.getByLabel("Senha").fill("1");
    await page.getByRole("button", { name: "Entrar" }).click();

    await expect(page.getByText("Usuário ou senha inválidos")).toBeVisible();
    await expect(page).toHaveURL(/\/login$/);

    const cookies = await context.cookies();
    expect(cookies.map((c) => c.name)).not.toContain("lmf_token");
  });
});
