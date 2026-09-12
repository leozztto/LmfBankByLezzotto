import { apiFetch } from "@/lib/api/client";
import type { AccountCreatePayload } from "@/lib/schemas/account";
import {
  accountListSchema,
  accountLookupResponseSchema,
  accountResponseSchema,
  type AccountLookupResponse,
  type AccountResponse,
} from "@/lib/schemas/responses";

export function createAccount(
  payload: AccountCreatePayload,
): Promise<AccountResponse> {
  return apiFetch(
    "accounts",
    { method: "POST", body: payload },
    accountResponseSchema,
  );
}

export function getAccount(id: number): Promise<AccountResponse> {
  return apiFetch(`accounts/${id}`, {}, accountResponseSchema);
}

export function getAccountByDocument(doc: string): Promise<AccountResponse> {
  return apiFetch(
    `accounts/document/${encodeURIComponent(doc)}`,
    {},
    accountResponseSchema,
  );
}

export function listAccounts(): Promise<AccountResponse[]> {
  return apiFetch("accounts", {}, accountListSchema);
}

/** Resolves a transfer destination by account number — cross-account, unrestricted (ADR 0010). */
export function getAccountByNumber(
  accountNumber: string,
): Promise<AccountLookupResponse> {
  return apiFetch(
    `accounts/number/${encodeURIComponent(accountNumber)}`,
    {},
    accountLookupResponseSchema,
  );
}

export type { AccountLookupResponse, AccountResponse };
