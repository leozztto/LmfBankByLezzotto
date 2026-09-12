"use client";

import { useState } from "react";

import { useStatement } from "@/hooks/use-statement";
import { useUiStore } from "@/stores/ui-store";
import type { DateRange } from "@/lib/api/statement";
import { txnStatusLabels, txnTypeLabels } from "@/lib/enums";
import { formatDateTime } from "@/lib/format";
import { AccountSelect } from "@/components/account-select";
import { DateRangeFilter } from "@/components/date-range-filter";
import { Money } from "@/components/money";
import { ApiErrorAlert } from "@/components/api-error-alert";
import { EmptyState } from "@/components/empty-state";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function StatementTable() {
  const selectedAccountId = useUiStore((s) => s.selectedAccountId);
  const setSelectedAccount = useUiStore((s) => s.setSelectedAccount);
  const [accountId, setAccountId] = useState<number | null>(selectedAccountId);
  const [range, setRange] = useState<DateRange | null>(null);

  const statement = useStatement(accountId ?? 0, range ?? undefined);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end gap-4">
        <div className="min-w-[16rem] space-y-1">
          <span className="text-sm font-medium">Conta</span>
          <AccountSelect
            value={accountId}
            onChange={(id) => {
              setAccountId(id);
              setSelectedAccount(id);
            }}
          />
        </div>
        <DateRangeFilter value={range} onChange={setRange} />
      </div>

      {!accountId ? (
        <EmptyState
          title="Selecione uma conta"
          description="Escolha a conta para ver o extrato."
        />
      ) : (
        <>
          <Card>
            <CardHeader>
              <CardDescription>Saldo atual</CardDescription>
              <CardTitle className="text-2xl">
                {statement.isLoading ? (
                  <Skeleton className="h-8 w-40" />
                ) : (
                  <Money value={statement.data?.balance ?? "0"} />
                )}
              </CardTitle>
            </CardHeader>
          </Card>

          {statement.isError && <ApiErrorAlert error={statement.error} />}

          {statement.isLoading ? (
            <Skeleton className="h-48 w-full" />
          ) : (statement.data?.transactions.length ?? 0) === 0 ? (
            <EmptyState
              title="Sem lançamentos"
              description={
                range
                  ? "Nenhum lançamento no período."
                  : "Esta conta ainda não tem lançamentos."
              }
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Data</TableHead>
                  <TableHead>Descrição</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Valor</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {statement.data!.transactions.map((t) => (
                  <TableRow key={t.transactionId}>
                    <TableCell className="whitespace-nowrap tabular-nums">
                      {formatDateTime(t.createdAt)}
                    </TableCell>
                    <TableCell>{t.description}</TableCell>
                    <TableCell>{txnTypeLabels[t.type]}</TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          t.status === "COMPLETED" ? "secondary" : "destructive"
                        }
                      >
                        {txnStatusLabels[t.status]}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Money
                        value={t.type === "DEBIT" ? `-${t.amount}` : t.amount}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </>
      )}
    </div>
  );
}
