import { test, expect } from "../fixtures/app";
import { brl } from "../fixtures/data";
import { submitMovement, openStatement } from "../fixtures/flows";

test("depósito → saldo e extrato refletem o valor", async ({ app }) => {
  const { page, api } = app;
  const account = await api.createAccount();

  await submitMovement(page, {
    accountId: account.accountId,
    amountCents: "15000", // R$ 150,00
    description: "Salario E2E",
  });

  // o card "Saldo da conta" na própria tela atualiza após o sucesso
  await expect(page.getByText(brl(150)).first()).toBeVisible();

  // e o lançamento aparece no extrato como crédito
  await openStatement(page, account.accountId);
  const row = page.getByRole("row").filter({ hasText: "Salario E2E" });
  await expect(row).toBeVisible();
  await expect(row.getByRole("cell", { name: "Crédito" })).toBeVisible();
  await expect(row.getByRole("cell", { name: brl(150) })).toBeVisible();
});
