import { test, expect } from "../fixtures/app";
import { brl } from "../fixtures/data";
import { submitTransfer, openStatement } from "../fixtures/flows";

test("transferência entre 2 contas → saldos e extratos coerentes dos dois lados", async ({
  app,
}) => {
  const { page, api } = app;
  const from = await api.createAccount();
  const to = await api.createAccount();
  await api.deposit(from.accountId, 300, "Aporte inicial");

  await submitTransfer(page, {
    fromAccountId: from.accountId,
    toAccountId: to.accountId,
    amountCents: "10000", // R$ 100,00
  });
  // a transferência limpa o campo valor no sucesso
  await expect(page.getByLabel("Valor")).toHaveValue("");

  // origem: 300 - 100 = 200, com um débito no extrato
  await openStatement(page, from.accountId);
  await expect(page.getByText(brl(200)).first()).toBeVisible();
  await expect(
    page.getByRole("row").filter({ hasText: "Débito" }),
  ).toBeVisible();

  // destino: 0 + 100 = 100, com um crédito no extrato
  await openStatement(page, to.accountId);
  await expect(page.getByText(brl(100)).first()).toBeVisible();
  await expect(
    page.getByRole("row").filter({ hasText: "Crédito" }),
  ).toBeVisible();
});
