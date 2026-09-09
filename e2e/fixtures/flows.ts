import { expect, type Page } from "@playwright/test";

import type { AccountFormData } from "./data";

/** MaskedInput é controlado e reformata a cada tecla — digitar char a char. */
async function typeMasked(
  page: Page,
  label: string,
  text: string,
): Promise<void> {
  const field = page.getByLabel(label, { exact: true });
  await field.click();
  await field.pressSequentially(text);
}

/** Login pela UI: preenche o form e espera cair no dashboard. */
export async function loginViaUi(
  page: Page,
  username = "e2e",
  password = "e2e",
): Promise<void> {
  await page.goto("/login");
  await page.getByLabel("Usuário").fill(username);
  await page.getByLabel("Senha").fill(password);
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/dashboard$/);
}

/**
 * Preenche o formulário de abertura de conta. Os `Select` (nacionalidade, tipo
 * de conta, tipo de endereço) já vêm com o default válido — não são tocados.
 */
export async function fillAccountForm(
  page: Page,
  data: AccountFormData,
): Promise<void> {
  await page.getByLabel("Nome completo").fill(data.fullName);
  await typeMasked(page, "CPF", data.cpf);
  await page.getByLabel("Data de nascimento").fill(data.birthDate);
  await page.getByLabel("Nome da mãe").fill(data.motherName);
  await page.getByLabel("E-mail").fill(data.email);
  await typeMasked(page, "Telefone", data.phone);
  await page.getByLabel("Profissão").fill(data.profession);
  await typeMasked(page, "Renda mensal", data.monthlyIncome);

  await typeMasked(page, "CEP", data.address.zipCode);
  await page.getByLabel("Logradouro").fill(data.address.street);
  await page.getByLabel("Bairro").fill(data.address.neighborhood);
  await page.getByLabel("Número", { exact: true }).fill(data.address.number);
  await page.getByLabel("Cidade").fill(data.address.city);
  await page.getByLabel("UF").fill(data.address.state);

  await page.getByLabel("Aceito os termos de uso e abertura de conta").check();
}

/** Clica o botão de confirmação dentro do dialog de confirmação. */
export async function confirm(page: Page, label: string): Promise<void> {
  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByRole("button", { name: label }).click();
  await expect(dialog).toBeHidden();
}

/** Depósito ou saque pela tela /deposit-withdraw. `amount` em centavos. */
export async function submitMovement(
  page: Page,
  opts: {
    accountId: number;
    amountCents: string;
    description: string;
    type?: "deposito" | "saque";
  },
): Promise<void> {
  const isWithdraw = opts.type === "saque";
  await page.goto("/deposit-withdraw");
  if (isWithdraw) await page.getByRole("tab", { name: "Saque" }).click();

  await page
    .getByLabel("Conta", { exact: true })
    .selectOption(String(opts.accountId));
  await typeMasked(page, "Valor", opts.amountCents);
  await page.getByLabel("Descrição").fill(opts.description);

  const action = isWithdraw ? "Sacar" : "Depositar";
  await page.getByRole("button", { name: action, exact: true }).click();
  await confirm(page, action);
}

/** Transferência pela tela /transfer. `amount` em centavos. */
export async function submitTransfer(
  page: Page,
  opts: { fromAccountId: number; toAccountId: number; amountCents: string },
): Promise<void> {
  await page.goto("/transfer");
  await page
    .getByLabel("Conta de origem")
    .selectOption(String(opts.fromAccountId));
  await page
    .getByLabel("Conta de destino")
    .selectOption(String(opts.toAccountId));
  await typeMasked(page, "Valor", opts.amountCents);
  await page.getByRole("button", { name: "Transferir", exact: true }).click();
  await confirm(page, "Transferir");
}

/** Abre /statement e seleciona a conta (o select não tem label associada). */
export async function openStatement(
  page: Page,
  accountId: number,
): Promise<void> {
  await page.goto("/statement");
  await page.getByRole("combobox").selectOption(String(accountId));
}
