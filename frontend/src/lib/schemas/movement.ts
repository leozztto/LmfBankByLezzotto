import { z } from "zod";

import { parseMoney } from "@/lib/schemas/common";
import { zTxnTypeReq } from "@/lib/enums";

/**
 * FORM schema for a deposit/withdraw. No transforms (RHF-friendly);
 * `toTransactionPayload` normalises to the POST /transactions body.
 */
export const transactionFormSchema = z.object({
  accountId: z.number().int().positive("Selecione uma conta"),
  type: zTxnTypeReq, // "C" = depósito, "D" = saque
  amount: z
    .string()
    .min(1, "Informe o valor")
    .refine((v) => {
      const n = parseMoney(v);
      return Number.isFinite(n) && n >= 0.01;
    }, "Valor mínimo R$ 0,01"),
  description: z.string().min(1, "Informe a descrição"),
});

export type TransactionFormValues = z.infer<typeof transactionFormSchema>;

export interface TransactionPayload {
  accountId: number;
  type: "C" | "D";
  amount: number;
  description: string;
  idempotencyKey: string;
}

export function toTransactionPayload(
  v: TransactionFormValues,
  idempotencyKey: string,
): TransactionPayload {
  return {
    accountId: v.accountId,
    type: v.type,
    amount: parseMoney(v.amount),
    description: v.description.trim(),
    idempotencyKey,
  };
}
