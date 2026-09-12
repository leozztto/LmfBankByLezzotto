"use client";

import Link from "next/link";

import { useAccountsQuery } from "@/hooks/use-accounts";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export function DashboardSummary() {
  const { data, isLoading } = useAccountsQuery();

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      <Card>
        <CardHeader>
          <CardDescription>Contas registradas</CardDescription>
          <CardTitle className="text-3xl">
            {isLoading ? (
              <Skeleton className="h-8 w-12" />
            ) : (
              (data?.length ?? 0)
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="text-sm">
          <Link href="/accounts" className="underline underline-offset-4">
            Ver todas
          </Link>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Ações rápidas</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-2 text-sm">
          <Link href="/open-account" className="underline underline-offset-4">
            Abrir uma conta
          </Link>
          <Link
            href="/deposit-withdraw"
            className="underline underline-offset-4"
          >
            Depósito / Saque
          </Link>
          <Link href="/transfer" className="underline underline-offset-4">
            Transferência
          </Link>
        </CardContent>
      </Card>
    </div>
  );
}
