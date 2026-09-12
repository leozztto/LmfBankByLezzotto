import { MatchersV3 } from "@pact-foundation/pact";
import { describe, expect, it, vi } from "vitest";

import { bankStatementResponseSchema } from "@/lib/schemas/responses";
import { callApi } from "./support/bff";
import { BEARER_EXAMPLE, newPact } from "./support/pact";

const { like, eachLike, integer, string, regex, nullValue, fromProviderState } =
  MatchersV3;

vi.mock("next/headers", () => ({
  cookies: () => ({
    get: (n: string) => (n === "lmf_token" ? { value: "the-jwt" } : undefined),
  }),
}));

const JSON_CT = regex("application/json.*", "application/json");
const AUTH = regex("Bearer .+", BEARER_EXAMPLE);

describe("Pact · lmfbank-frontend → lmfbank-backend · statement", () => {
  const pact = newPact();

  it("GET /accounts/statement devolve BankStatementResponse (sem intervalo)", async () => {
    pact
      .given("an account exists with a completed transaction")
      .uponReceiving("a request for the full statement of an account")
      .withRequest({
        method: "GET",
        path: "/accounts/statement",
        query: { accountId: fromProviderState("${accountId}", "1") },
        headers: { Authorization: AUTH },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": JSON_CT },
        body: {
          accountId: integer(1),
          balance: like(150.0),
          startDate: nullValue(),
          endDate: nullValue(),
          transactions: eachLike({
            transactionId: string("2b1c9f6e-0a1b-4c2d-9e3f-4a5b6c7d8e9f"),
            accountId: integer(1),
            type: regex("CREDIT|DEBIT", "CREDIT"),
            amount: like(150.0),
            status: regex("PENDING|COMPLETED|FAILED", "COMPLETED"),
            description: string("Opening balance"),
            createdAt: string("2026-09-09T12:34:56.789"),
            transferId: nullValue(),
          }),
        },
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "GET", "accounts/statement", {
        search: "accountId=1",
      });
      const body = await res.json();
      expect(res.status).toBe(200);
      expect(() => bankStatementResponseSchema.parse(body)).not.toThrow();
    });
  });
});
