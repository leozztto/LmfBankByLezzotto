import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { createTransaction } from "./movements";
import type { TransactionPayload } from "@/lib/schemas/movement";

const payload: TransactionPayload = {
  accountId: 1,
  type: "D",
  amount: 50,
  description: "saque",
  idempotencyKey: "k1",
};

describe("createTransaction", () => {
  it("posts the payload and parses the response", async () => {
    let sent: unknown;
    server.use(
      http.post("/api/transactions", async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(
          {
            transactionId: "t1",
            accountId: 1,
            type: "DEBIT",
            amount: 50,
            status: "COMPLETED",
            description: "saque",
            createdAt: "2026-09-08T10:00:00",
            transferId: null,
          },
          { status: 201 },
        );
      }),
    );

    const txn = await createTransaction(payload);
    expect(sent).toEqual(payload);
    expect(txn.type).toBe("DEBIT");
    expect(txn.amount).toBe("50");
  });

  it("propagates a 422 INSUFFICIENT_BALANCE", async () => {
    server.use(
      http.post("/api/transactions", () =>
        HttpResponse.json(
          {
            status: 422,
            code: "INSUFFICIENT_BALANCE",
            message: "Saldo insuficiente para conta 1. Saldo atual: 20, valor solicitado: 50",
          },
          { status: 422 },
        ),
      ),
    );

    await expect(createTransaction(payload)).rejects.toMatchObject({
      status: 422,
      code: "INSUFFICIENT_BALANCE",
    });
  });
});
