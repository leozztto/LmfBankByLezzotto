import { apiFetch } from "@/lib/api/client";
import type {
  TransactionPayload,
  TransferPayload,
} from "@/lib/schemas/movement";
import {
  transactionResponseSchema,
  transferResponseSchema,
  type TransactionResponse,
  type TransferResponse,
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

/**
 * POST /transfers. On an idempotent replay the backend returns 201 with the
 * ORIGINAL transfer (no 409) — the caller must check `status`/`failureReason`
 * to know it actually succeeded.
 */
export function createTransfer(
  payload: TransferPayload,
): Promise<TransferResponse> {
  return apiFetch(
    "transfers",
    { method: "POST", body: payload },
    transferResponseSchema,
  );
}

export type { TransactionResponse, TransferResponse };
