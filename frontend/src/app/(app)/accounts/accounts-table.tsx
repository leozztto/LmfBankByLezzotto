"use client";

import Link from "next/link";

import { useAccountsQuery } from "@/hooks/use-accounts";
import { accountStatusLabels, accountTypeLabels } from "@/lib/enums";
import { ApiErrorAlert } from "@/components/api-error-alert";
import { EmptyState } from "@/components/empty-state";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

export function AccountsTable() {
  const { data, isLoading, error } = useAccountsQuery();

  if (isLoading) {
    return <Skeleton className="h-48 w-full" />;
  }
  if (error) {
    return <ApiErrorAlert error={error} />;
  }
  if (!data || data.length === 0) {
    return (
      <EmptyState
        title="Nenhuma conta"
        description="Abra uma conta para começar."
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Conta</TableHead>
          <TableHead>Titular</TableHead>
          <TableHead>Documento</TableHead>
          <TableHead>Tipo</TableHead>
          <TableHead>Status</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.map((a) => (
          <TableRow key={a.accountId}>
            <TableCell>
              <Link
                href={`/accounts/${a.accountId}`}
                className="font-medium underline-offset-4 hover:underline"
              >
                {a.accountNumber}
              </Link>
              <span className="block text-xs text-muted-foreground">
                ag. {a.agency}
              </span>
            </TableCell>
            <TableCell>{a.fullName}</TableCell>
            <TableCell className="tabular-nums">{a.maskedDocument}</TableCell>
            <TableCell>{accountTypeLabels[a.accountType]}</TableCell>
            <TableCell>
              <Badge
                variant={a.accountStatus === "A" ? "secondary" : "destructive"}
              >
                {accountStatusLabels[a.accountStatus]}
              </Badge>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
