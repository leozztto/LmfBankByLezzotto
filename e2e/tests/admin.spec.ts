import { test, expect } from "../fixtures/app";
import { test as guestTest, expect as guestExpect } from "@playwright/test";
import { loginViaUi } from "../fixtures/flows";

test.describe("admin", () => {
  test("admin cria um usuário e vincula a uma conta (ADR 0010)", async ({
    app,
  }) => {
    const { page, api } = app;
    const account = await api.createAccount();
    const username = `e2e-admin-${Date.now()}`;

    await page.goto("/admin");
    await expect(page.getByRole("heading", { name: "Admin" })).toBeVisible();

    await page.getByLabel("Usuário", { exact: true }).fill(username);
    await page.getByLabel("Senha").fill("senha123");
    await page.getByRole("button", { name: "Criar usuário" }).click();

    // form de criação limpa no sucesso
    await expect(page.getByLabel("Usuário", { exact: true })).toHaveValue("");

    await page.getByLabel("Usuário a vincular").fill(username);
    await page.getByLabel("Id da conta").fill(String(account.accountId));
    await page.getByRole("button", { name: "Vincular conta" }).click();

    await expect(page.getByLabel("Usuário a vincular")).toHaveValue("");
  });
});

guestTest(
  "usuário comum não acessa /admin — redireciona pro dashboard (ADR 0010)",
  async ({ page }) => {
    await loginViaUi(page);
    await page.goto("/admin");
    await guestExpect(page).toHaveURL(/\/dashboard$/);
  },
);
