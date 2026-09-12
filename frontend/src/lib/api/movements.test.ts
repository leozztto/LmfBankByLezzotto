import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { createTransaction, createTransfer } from "./movements";
import type {
  TransactionPayload,
  TransferPayload,
} from "@/lib/schemas/movement";

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
            message:
              "Saldo insuficiente para conta 1. Saldo atual: 20, valor solicitado: 50",
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

const transferPayload: TransferPayload = {
  fromAccountId: 1,
  toAccountId: 2,
  amount: 100,
  idempotencyKey: "uuid-1",
};

describe("createTransfer", () => {
  it("posts and parses the response (money normalised)", async () => {
    let sent: unknown;
    server.use(
      http.post("/api/transfers", async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json(
          {
            transferId: "tr1",
            fromAccountId: 1,
            toAccountId: 2,
            amount: 100,
            status: "COMPLETED",
            createdAt: "2026-09-08T10:00:00",
            failureReason: null,
          },
          { status: 201 },
        );
      }),
    );

    const res = await createTransfer(transferPayload);
    expect(sent).toEqual(transferPayload);
    expect(res.amount).toBe("100");
    expect(res.status).toBe("COMPLETED");
  });

  it("propagates a 400 same-account and a 403 blocked", async () => {
    server.use(
      http.post("/api/transfers", () =>
        HttpResponse.json(
          {
            status: 400,
            code: "BAD_REQUEST",
            message: "Source and destination accounts must be different",
          },
          { status: 400 },
        ),
      ),
    );
    await expect(createTransfer(transferPayload)).rejects.toMatchObject({
      status: 400,
      code: "BAD_REQUEST",
    });
  });
});
