import { randomUUID } from "node:crypto";
import {
  test as base,
  expect,
  type BrowserContext,
  type Page,
} from "@playwright/test";

import { buildAccountPayload, type AccountPayload } from "./data";

export interface CreatedAccount {
  accountId: number;
  accountNumber: string;
  fullName: string;
}

export interface StatementTxn {
  type: "CREDIT" | "DEBIT";
  amount: string | number;
  status: string;
  description: string;
}

export interface Statement {
  balance: string | number;
  transactions: StatementTxn[];
}

/**
 * Helpers de API que passam pelo BFF (`/api/*`, same-origin), reusando o cookie
 * httpOnly já setado no contexto — o mesmo caminho do navegador (ADR 0007).
 * Usados só para PREPARAR o estado; as asserções dos specs são sempre pela UI.
 */
export interface Api {
  createAccount(overrides?: Partial<AccountPayload>): Promise<CreatedAccount>;
  deposit(
    accountId: number,
    amount: number,
    description?: string,
  ): Promise<void>;
  getStatement(accountId: number): Promise<Statement>;
}

export interface App {
  page: Page;
  api: Api;
  context: BrowserContext;
}

export const test = base.extend<{ app: App }>({
  app: async ({ browser, baseURL }, use) => {
    const context = await browser.newContext({ baseURL });

    // ADMIN (ADR 0010): estes specs criam contas ad-hoc e operam nelas
    // diretamente (depósito, transferência, extrato) — não são donos delas sob
    // o modelo de escopo, e o seletor de destino em dropdown (usado abaixo via
    // `.selectOption`) só existe pra admin. "demo" fica pro login.spec.ts, que
    // testa a experiência de um usuário comum de verdade.
    const login = await context.request.post("/api/auth/login", {
      data: { username: "admin", password: "admin" },
    });
    expect(
      login.ok(),
      `login falhou: ${login.status()} ${await login.text()}`,
    ).toBeTruthy();

    const rc = context.request;

    const api: Api = {
      async createAccount(overrides) {
        const res = await rc.post("/api/accounts", {
          data: buildAccountPayload(overrides),
        });
        expect(
          res.ok(),
          `createAccount falhou: ${res.status()} ${await res.text()}`,
        ).toBeTruthy();
        const body = await res.json();
        return {
          accountId: body.accountId,
          accountNumber: body.accountNumber,
          fullName: body.fullName,
        };
      },

      async deposit(accountId, amount, description = "E2E deposito") {
        const res = await rc.post("/api/transactions", {
          data: {
            accountId,
            type: "C",
            amount,
            description,
            idempotencyKey: randomUUID(),
          },
        });
        expect(
          res.ok(),
          `deposit falhou: ${res.status()} ${await res.text()}`,
        ).toBeTruthy();
      },

      async getStatement(accountId) {
        const res = await rc.get(
          `/api/accounts/statement?accountId=${accountId}`,
        );
        expect(res.ok()).toBeTruthy();
        return res.json();
      },
    };

    const page = await context.newPage();
    await use({ page, api, context });
    await context.close();
  },
});

export { expect };
