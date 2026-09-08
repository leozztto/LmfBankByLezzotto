import { describe, expect, it } from "vitest";

import {
  accountResponseSchema,
  bankStatementResponseSchema,
} from "./responses";

const account = (balance: unknown) => ({
  accountId: 1,
  fullName: "Maria",
  maskedDocument: "***",
  maskedEmail: "m***@e.com",
  maskedPhone: "(11) *****-1",
  accountType: "C",
  accountNumber: "1-2",
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: balance, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
});

describe("accountResponseSchema", () => {
  it("normalises money from a JSON number to a string", () => {
    const parsed = accountResponseSchema.parse(account(0));
    expect(parsed.balance.availableBalance).toBe("0");
    expect(parsed.balance.blockedBalance).toBe("0");
  });

  it("also accepts money already sent as a string", () => {
    const parsed = accountResponseSchema.parse(account("123.45"));
    expect(parsed.balance.availableBalance).toBe("123.45");
  });

  it("rejects an unknown accountStatus code", () => {
    expect(() =>
      accountResponseSchema.parse({ ...account(0), accountStatus: "X" }),
    ).toThrow();
  });
});

describe("bankStatementResponseSchema", () => {
  it("normalises balance and transaction amounts", () => {
    const parsed = bankStatementResponseSchema.parse({
      accountId: 1,
      balance: 99.9,
      startDate: null,
      endDate: null,
      transactions: [
        {
          transactionId: "t",
          accountId: 1,
          type: "DEBIT",
          amount: 10,
          status: "COMPLETED",
          description: "Withdraw",
          createdAt: "2026-09-08T10:00:00",
          transferId: null,
        },
      ],
    });
    expect(parsed.balance).toBe("99.9");
    expect(parsed.transactions[0].amount).toBe("10");
    expect(parsed.transactions[0].type).toBe("DEBIT");
  });
});
