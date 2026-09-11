"use client";

import { useQuery } from "@tanstack/react-query";

import {
  getAccount,
  getAccountByDocument,
  getAccountByNumber,
  listAccounts,
} from "@/lib/api/accounts";
import { onlyDigits } from "@/lib/schemas/common";
import { queryKeys } from "@/lib/query/keys";

export function useAccountsQuery() {
  return useQuery({
    queryKey: queryKeys.accounts.list(),
    queryFn: listAccounts,
  });
}

export function useAccountQuery(id: number) {
  return useQuery({
    queryKey: queryKeys.accounts.detail(id),
    queryFn: () => getAccount(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useAccountByDocumentQuery(document: string) {
  const doc = onlyDigits(document);
  return useQuery({
    queryKey: queryKeys.accounts.byDocument(doc),
    queryFn: () => getAccountByDocument(doc),
    enabled: doc.length === 11,
    retry: false,
  });
}

/** `AccountNumberGenerator` (backend) always emits exactly 8 digits + "-" + 1 check digit. */
const ACCOUNT_NUMBER_SHAPE = /^\d{8}-\d$/;

/** Resolves a transfer destination by account number (ADR 0010) — for regular users, who
 *  can't list every account to pick one from a dropdown like an admin can. Only fires once
 *  the typed value has the complete shape, so it doesn't fire the (unrestricted,
 *  cross-account) lookup on every keystroke of a partial number. */
export function useAccountByNumberQuery(accountNumber: string) {
  return useQuery({
    queryKey: queryKeys.accounts.byNumber(accountNumber),
    queryFn: () => getAccountByNumber(accountNumber),
    enabled: ACCOUNT_NUMBER_SHAPE.test(accountNumber),
    retry: false,
  });
}
