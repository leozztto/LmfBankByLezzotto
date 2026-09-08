"use client";

import { useQuery } from "@tanstack/react-query";

import {
  getAccount,
  getAccountByDocument,
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
