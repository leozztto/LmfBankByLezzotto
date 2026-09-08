import { describe, expect, it } from "vitest";

import {
  toTransactionPayload,
  transactionFormSchema,
  type TransactionFormValues,
} from "./movement";

const valid: TransactionFormValues = {
  accountId: 3,
  type: "C",
  amount: "R$ 150,00",
  description: "aluguel",
};

describe("transactionFormSchema", () => {
  it("accepts a valid deposit form", () => {
    expect(transactionFormSchema.safeParse(valid).success).toBe(true);
  });

  it("requires an account, a value >= 0.01 and a description", () => {
    const r = transactionFormSchema.safeParse({
      accountId: 0,
      type: "D",
      amount: "R$ 0,00",
      description: "",
    });
    expect(r.success).toBe(false);
    const paths = r.success ? [] : r.error.issues.map((i) => i.path.join("."));
    expect(paths).toEqual(
      expect.arrayContaining(["accountId", "amount", "description"]),
    );
  });
});

describe("toTransactionPayload", () => {
  it("parses money to a number and carries the idempotency key", () => {
    const payload = toTransactionPayload(valid, "key-123");
    expect(payload).toEqual({
      accountId: 3,
      type: "C",
      amount: 150,
      description: "aluguel",
      idempotencyKey: "key-123",
    });
  });
});
