import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import {
  createAccount,
  getAccount,
  getAccountByDocument,
  listAccounts,
} from "./accounts";
import type { AccountCreatePayload } from "@/lib/schemas/account";

const accountJson = (id: number) => ({
  accountId: id,
  fullName: "Maria Silva",
  maskedDocument: "***.982.***-25",
  maskedEmail: "ma***@example.com",
  maskedPhone: "(11) *****-4321",
  accountType: "C",
  accountNumber: "12345678-9",
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: "0", blockedBalance: "0", totalBalance: "0" },
  addresses: [],
});

const payload: AccountCreatePayload = {
  fullName: "Maria Silva",
  documentNumber: "52998224725",
  birthDate: "1990-05-20",
  motherName: "Joana",
  nationality: "BR",
  email: "m@e.com",
  phone: "11987654321",
  profession: "Eng",
  monthlyIncome: 12000,
  accountType: "C",
  acceptedTerms: true,
  addresses: [],
};

describe("accounts api", () => {
  it("createAccount posts the payload and parses the response", async () => {
    let sent: unknown;
    server.use(
      http.post("/api/accounts", async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(accountJson(1), { status: 201 });
      }),
    );
    const acc = await createAccount(payload);
    expect(sent).toEqual(payload);
    expect(acc.accountId).toBe(1);
    expect(acc.accountStatus).toBe("A");
  });

  it("getAccount / getAccountByDocument / listAccounts hit the right paths", async () => {
    server.use(
      http.get("/api/accounts/7", () => HttpResponse.json(accountJson(7))),
      http.get("/api/accounts/document/52998224725", () =>
        HttpResponse.json(accountJson(8)),
      ),
      http.get("/api/accounts", () =>
        HttpResponse.json([accountJson(1), accountJson(2)]),
      ),
    );
    expect((await getAccount(7)).accountId).toBe(7);
    expect((await getAccountByDocument("52998224725")).accountId).toBe(8);
    expect(await listAccounts()).toHaveLength(2);
  });

  it("propagates a 409 as an ApiError", async () => {
    server.use(
      http.post("/api/accounts", () =>
        HttpResponse.json(
          { status: 409, code: "DOCUMENT_ALREADY_EXISTS", message: "já existe" },
          { status: 409 },
        ),
      ),
    );
    await expect(createAccount(payload)).rejects.toMatchObject({
      status: 409,
      code: "DOCUMENT_ALREADY_EXISTS",
    });
  });
});
