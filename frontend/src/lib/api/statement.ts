import { apiFetch } from "@/lib/api/client";
import {
  bankStatementResponseSchema,
  type BankStatementResponse,
} from "@/lib/schemas/responses";

export interface DateRange {
  start: string;
  end: string;
}

/**
 * GET /accounts/statement?accountId&startDate&endDate. Without a range the
 * backend returns the full history (newest first). `balance` is always all-time.
 */
export function getStatement(
  accountId: number,
  range?: DateRange,
): Promise<BankStatementResponse> {
  const params = new URLSearchParams({ accountId: String(accountId) });
  if (range) {
    params.set("startDate", range.start);
    params.set("endDate", range.end);
  }
  return apiFetch(
    `accounts/statement?${params.toString()}`,
    {},
    bankStatementResponseSchema,
  );
}

export type { BankStatementResponse };
