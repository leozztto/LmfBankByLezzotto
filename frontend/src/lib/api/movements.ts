import { apiFetch } from "@/lib/api/client";
import type { TransactionPayload } from "@/lib/schemas/movement";
import {
  transactionResponseSchema,
  type TransactionResponse,
} from "@/lib/schemas/responses";

/**
 * POST /transactions. `idempotencyKey` is part of the payload (built by the
 * caller so a retry reuses it). After Fase 3.0 the backend actually dedupes on
 * it, but the UI still guards double-submit.
 */
export function createTransaction(
  payload: TransactionPayload,
): Promise<TransactionResponse> {
  return apiFetch(
    "transactions",
    { method: "POST", body: payload },
    transactionResponseSchema,
  );
}

export type { TransactionResponse };
