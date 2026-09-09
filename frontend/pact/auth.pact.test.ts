import { MatchersV3 } from "@pact-foundation/pact";
import { describe, expect, it, vi } from "vitest";

import { callLogin } from "./support/bff";
import { newPact } from "./support/pact";

const { string, integer, regex } = MatchersV3;

// O route de login grava o cookie de sessão; o mock precisa de get + set.
vi.mock("next/headers", () => {
  const store = new Map<string, string>();
  return {
    cookies: () => ({
      get: (n: string) => {
        const v = store.get(n);
        return v ? { value: v } : undefined;
      },
      set: (n: string, v: string) => {
        store.set(n, v);
      },
    }),
  };
});

const JSON_CT = regex("application/json.*", "application/json");

describe("Pact · lmfbank-frontend → lmfbank-backend · auth", () => {
  const pact = newPact();

  it("POST /auth/login devolve token, tokenType e expiresIn (loginResponseSchema)", async () => {
    pact
      .given("credentials are accepted")
      .uponReceiving("a login request")
      .withRequest({
        method: "POST",
        path: "/auth/login",
        headers: { "Content-Type": JSON_CT },
        body: { username: "demo", password: "demo" },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": JSON_CT },
        body: {
          token: string(
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vIiwiZXhwIjo0MTAyNDQ0ODAwfQ.sig",
          ),
          tokenType: string("Bearer"),
          expiresIn: integer(86400),
        },
      });

    await pact.executeTest(async (mock) => {
      const res = await callLogin(mock.url, {
        username: "demo",
        password: "demo",
      });

      // 200 (e não 502) prova que loginResponseSchema.parse aceitou a resposta
      // do backend e o BFF conseguiu montar a sessão.
      expect(res.status).toBe(200);
      await expect(res.json()).resolves.toMatchObject({
        authenticated: true,
        username: "demo",
      });
    });
  });
});
