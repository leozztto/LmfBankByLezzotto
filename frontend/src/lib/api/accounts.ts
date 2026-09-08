import { apiFetch } from "@/lib/api/client";
import type { AccountCreatePayload } from "@/lib/schemas/account";
import {
  accountListSchema,
  accountResponseSchema,
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

export type { AccountResponse };
