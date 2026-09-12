import { test, expect } from "../fixtures/app";
import { buildAccountFormData, brl } from "../fixtures/data";
import { fillAccountForm } from "../fixtures/flows";

test("abrir conta pela UI → aparece na listagem com saldo zero", async ({
  app,
}) => {
  const { page } = app;
  const data = buildAccountFormData();

  await page.goto("/open-account");
  await fillAccountForm(page, data);
  await page.getByRole("button", { name: "Criar conta" }).click();

  // useCreateAccount redireciona para /accounts/{id}
  await expect(page).toHaveURL(/\/accounts\/\d+$/);
  await expect(
    page.getByRole("heading", { name: data.fullName }),
  ).toBeVisible();

  // saldo disponível da conta recém-criada = R$ 0,00
  await expect(page.getByText(brl(0), { exact: true })).toBeVisible();

  // e a conta aparece na listagem
  await page.goto("/accounts");
  await expect(page.getByRole("cell", { name: data.fullName })).toBeVisible();
});
