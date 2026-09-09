import { test, expect } from "../fixtures/app";
import { submitTransfer } from "../fixtures/flows";

/**
 * Valida o contrato `ApiError` ({status, code, message}) atravessando
 * backend → BFF → `ApiErrorAlert`. Se o backend renomear `message`, o alerta
 * fica vazio e este spec falha.
 */
test("transferência sem saldo → o front mostra a mensagem de erro do backend", async ({
  app,
}) => {
  const { page, api } = app;
  const from = await api.createAccount();
  const to = await api.createAccount();

  await submitTransfer(page, {
    fromAccountId: from.accountId,
    toAccountId: to.accountId,
    amountCents: "5000", // R$ 50,00 — conta sem saldo
  });

  // getByRole("alert") também casa o route-announcer do Next — filtra pelo título
  const alert = page
    .getByRole("alert")
    .filter({ hasText: "Não foi possível concluir" });
  await expect(alert).toBeVisible();
  // texto exato vindo de InsufficientBalanceException no backend
  await expect(alert).toContainText(/Saldo insuficiente para conta \d+/);
});
