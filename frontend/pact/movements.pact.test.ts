import { MatchersV3 } from "@pact-foundation/pact";
import { describe, expect, it, vi } from "vitest";

import {
  transactionResponseSchema,
  transferResponseSchema,
} from "@/lib/schemas/responses";
import { callApi } from "./support/bff";
import { BEARER_EXAMPLE, newPact } from "./support/pact";

const {
  like,
  integer,
  number,
  string,
  regex,
  uuid,
  nullValue,
  fromProviderState,
} = MatchersV3;

vi.mock("next/headers", () => ({
  cookies: () => ({
    get: (n: string) => (n === "lmf_token" ? { value: "the-jwt" } : undefined),
  }),
}));

const JSON_CT = regex("application/json.*", "application/json");
const AUTH = regex("Bearer .+", BEARER_EXAMPLE);
const EXAMPLE_UUID = "3f1e6c9a-1b2c-4d5e-8f90-0a1b2c3d4e5f";

describe("Pact · lmfbank-frontend → lmfbank-backend · movements", () => {
  const pact = newPact();

  it("POST /transactions cria um CREDIT e devolve TransactionResponse", async () => {
    pact
      .given("an active account exists")
      .uponReceiving("a request to create a credit transaction")
      .withRequest({
        method: "POST",
        path: "/transactions",
        headers: { "Content-Type": JSON_CT, Authorization: AUTH },
        body: {
          accountId: fromProviderState("${accountId}", 1),
          type: "C",
          amount: 150.0,
          description: "Depósito",
          idempotencyKey: like("idem-credit-1"),
        },
      })
      .willRespondWith({
        status: 201,
        headers: { "Content-Type": JSON_CT },
        body: {
          transactionId: uuid("2b1c9f6e-0a1b-4c2d-9e3f-4a5b6c7d8e9f"),
          accountId: integer(1),
          type: regex("CREDIT|DEBIT", "CREDIT"),
          amount: number(150.0),
          status: regex("PENDING|COMPLETED|FAILED", "COMPLETED"),
          description: string("Depósito"),
          createdAt: string("2026-09-09T12:34:56.789"),
          transferId: nullValue(),
        },
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "POST", "transactions", {
        body: {
          accountId: 1,
          type: "C",
          amount: 150.0,
          description: "Depósito",
          idempotencyKey: "idem-credit-1",
        },
      });
      const body = await res.json();
      expect(res.status).toBe(201);
      expect(() => transactionResponseSchema.parse(body)).not.toThrow();
    });
  });

  it("POST /transfers move o valor e devolve TransferResponse", async () => {
    pact
      .given("two accounts exist for a transfer")
      .uponReceiving("a request to create a transfer")
      .withRequest({
        method: "POST",
        path: "/transfers",
        headers: { "Content-Type": JSON_CT, Authorization: AUTH },
        body: {
          fromAccountId: fromProviderState("${fromAccountId}", 1),
          toAccountId: fromProviderState("${toAccountId}", 2),
          amount: 125.0,
          idempotencyKey: uuid(EXAMPLE_UUID),
        },
      })
      .willRespondWith({
        status: 201,
        headers: { "Content-Type": JSON_CT },
        body: {
          transferId: uuid("9c8b7a6d-5e4f-4a3b-2c1d-0e9f8a7b6c5d"),
          fromAccountId: integer(1),
          toAccountId: integer(2),
          amount: number(125.0),
          status: regex("PENDING|COMPLETED|FAILED", "COMPLETED"),
          createdAt: string("2026-09-09T12:34:56.789"),
          failureReason: nullValue(),
        },
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "POST", "transfers", {
        body: {
          fromAccountId: 1,
          toAccountId: 2,
          amount: 125.0,
          idempotencyKey: EXAMPLE_UUID,
        },
      });
      const body = await res.json();
      expect(res.status).toBe(201);
      expect(() => transferResponseSchema.parse(body)).not.toThrow();
    });
  });
});
