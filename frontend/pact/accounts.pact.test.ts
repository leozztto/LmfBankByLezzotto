import { MatchersV3 } from "@pact-foundation/pact";
import { describe, expect, it, vi } from "vitest";

import {
  accountListSchema,
  accountResponseSchema,
} from "@/lib/schemas/responses";
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

/**
 * Campos exigidos por `accountResponseSchema` (src/lib/schemas/responses.ts),
 * com os matchers mais frouxos que o schema Zod aceita:
 * - `accountNumber` é `z.string()` no front (não checa formato) → `string()`;
 * - money é `string | number` no front → `like(number)`;
 * - `updatedAt` é `.nullable()` e `addresses` um array (aceita vazio), mas as
 *   CHAVES precisam existir na resposta — o schema quebra sem elas.
 */
const accountResponseBody = {
  accountId: integer(1),
  fullName: string("Maria Silva"),
  maskedDocument: string("***.456.***-01"),
  maskedEmail: string("ma*********@example.com"),
  maskedPhone: string("(11) *****-4321"),
  accountType: regex("C|S", "C"),
  accountNumber: string("00000001-2"),
  agency: string("0001"),
  accountStatus: regex("A|B|C", "A"),
  createdAt: string("2026-09-09T12:34:56.789"),
  updatedAt: nullValue(),
  balance: {
    availableBalance: like(0),
    blockedBalance: like(0),
    totalBalance: like(0),
  },
  addresses: eachLike(
    {
      id: integer(1),
      zipCode: string("01001000"),
      street: string("Praça da Sé"),
      addressType: regex("R|C|B", "R"),
    },
    0,
  ),
};

const createAccountBody = {
  fullName: "Maria Silva",
  documentNumber: "12345678901",
  birthDate: "1990-05-20",
  motherName: "Joana Silva",
  nationality: "BR",
  email: "maria.silva@example.com",
  phone: "11987654321",
  profession: "Engenheira",
  monthlyIncome: 12000,
  accountType: "C",
  acceptedTerms: true,
  addresses: [
    {
      zipCode: "01001000",
      street: "Praça da Sé",
      neighborhood: "Sé",
      number: "100",
      complement: "",
      city: "São Paulo",
      state: "SP",
      country: "BR",
      addressType: "R",
    },
  ],
};

describe("Pact · lmfbank-frontend → lmfbank-backend · accounts", () => {
  const pact = newPact();

  it("POST /accounts cria a conta e devolve AccountResponse", async () => {
    pact
      .given("a new account can be created")
      .uponReceiving("a request to open an account")
      .withRequest({
        method: "POST",
        path: "/accounts",
        headers: { "Content-Type": JSON_CT, Authorization: AUTH },
        body: createAccountBody,
      })
      .willRespondWith({
        status: 201,
        headers: { "Content-Type": JSON_CT },
        body: accountResponseBody,
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "POST", "accounts", {
        body: createAccountBody,
      });
      const body = await res.json();
      expect(res.status).toBe(201);
      expect(() => accountResponseSchema.parse(body)).not.toThrow();
    });
  });

  it("GET /accounts lista contas (accountListSchema)", async () => {
    pact
      .given("at least one account exists")
      .uponReceiving("a request to list accounts")
      .withRequest({
        method: "GET",
        path: "/accounts",
        headers: { Authorization: AUTH },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": JSON_CT },
        body: eachLike(accountResponseBody),
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "GET", "accounts");
      const body = await res.json();
      expect(res.status).toBe(200);
      expect(() => accountListSchema.parse(body)).not.toThrow();
    });
  });

  it("GET /accounts/{id} devolve a conta quando existe", async () => {
    pact
      .given("an account exists")
      .uponReceiving("a request for an account by id")
      .withRequest({
        method: "GET",
        path: fromProviderState("/accounts/${id}", "/accounts/1"),
        headers: { Authorization: AUTH },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": JSON_CT },
        body: accountResponseBody,
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "GET", "accounts/1");
      const body = await res.json();
      expect(res.status).toBe(200);
      expect(() => accountResponseSchema.parse(body)).not.toThrow();
    });
  });

  it("GET /accounts/{id} devolve 404 ACCOUNT_NOT_FOUND quando não existe", async () => {
    pact
      .given("no account exists with a given id")
      .uponReceiving("a request for a missing account")
      .withRequest({
        method: "GET",
        path: "/accounts/999999",
        headers: { Authorization: AUTH },
      })
      .willRespondWith({
        status: 404,
        headers: { "Content-Type": JSON_CT },
        body: {
          status: integer(404),
          code: string("ACCOUNT_NOT_FOUND"),
          message: string("Account not found"),
        },
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(mock.url, "GET", "accounts/999999");
      const body = await res.json();
      expect(res.status).toBe(404);
      expect(body).toMatchObject({ code: "ACCOUNT_NOT_FOUND" });
    });
  });

  it("GET /accounts/document/{documentNumber} devolve a conta", async () => {
    pact
      .given("an account exists with a known document")
      .uponReceiving("a request for an account by document")
      .withRequest({
        method: "GET",
        path: fromProviderState(
          "/accounts/document/${document}",
          "/accounts/document/12345678901",
        ),
        headers: { Authorization: AUTH },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": JSON_CT },
        body: accountResponseBody,
      });

    await pact.executeTest(async (mock) => {
      const res = await callApi(
        mock.url,
        "GET",
        "accounts/document/12345678901",
      );
      const body = await res.json();
      expect(res.status).toBe(200);
      expect(() => accountResponseSchema.parse(body)).not.toThrow();
    });
  });
});
