"use client";

import { useQuery } from "@tanstack/react-query";

import { getStatement, type DateRange } from "@/lib/api/statement";
import { queryKeys } from "@/lib/query/keys";

export function useStatement(accountId: number, range?: DateRange) {
  return useQuery({
    queryKey: queryKeys.statement(accountId, range),
    queryFn: () => getStatement(accountId, range),
    enabled: Number.isFinite(accountId) && accountId > 0,
  });
}
