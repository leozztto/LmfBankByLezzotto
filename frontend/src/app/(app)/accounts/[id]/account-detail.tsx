"use client";

import { useEffect } from "react";
import Link from "next/link";
import { notFound } from "next/navigation";

import { useAccountQuery } from "@/hooks/use-accounts";
import { useStatement } from "@/hooks/use-statement";
import { useUiStore } from "@/stores/ui-store";
import { ApiError } from "@/lib/api/errors";
import {
  accountStatusLabels,
  accountTypeLabels,
  nationalityLabels,
  txnTypeLabels,
} from "@/lib/enums";
import { formatDateTime } from "@/lib/format";
import { ApiErrorAlert } from "@/components/api-error-alert";
import { Money } from "@/components/money";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";

function Row({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className="flex justify-between gap-4 py-1.5 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <span className="text-right font-medium">{value}</span>
    </div>
  );
}

export function AccountDetail({ id }: { id: number }) {
  const account = useAccountQuery(id);
  const statement = useStatement(id);
  const setSelected = useUiStore((s) => s.setSelectedAccount);

  useEffect(() => {
    if (account.data) setSelected(id);
  }, [account.data, id, setSelected]);

  if (
    account.isError &&
    account.error instanceof ApiError &&
    account.error.code === "ACCOUNT_NOT_FOUND"
  ) {
    notFound();
    return null;
  }

  if (account.isLoading) {
    return <Skeleton className="h-72 w-full max-w-2xl" />;
  }
  if (account.isError || !account.data) {
    return <ApiErrorAlert error={account.error} />;
  }

  const a = account.data;
  const recent = statement.data?.transactions.slice(0, 5) ?? [];

  return (
    <div className="max-w-2xl space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold">{a.fullName}</h1>
          <p className="text-sm text-muted-foreground">
            Conta {a.accountNumber} · ag. {a.agency}
          </p>
        </div>
        <Badge variant={a.accountStatus === "A" ? "secondary" : "destructive"}>
          {accountStatusLabels[a.accountStatus]}
        </Badge>
      </div>

      <Card>
        <CardHeader>
          <CardDescription>Saldo disponível</CardDescription>
          <CardTitle className="text-3xl">
            {statement.isLoading ? (
              <Skeleton className="h-8 w-40" />
            ) : (
              <Money
                value={statement.data?.balance ?? a.balance.availableBalance}
              />
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <Button asChild variant="outline" size="sm">
            <Link href="/statement">Ver extrato completo</Link>
          </Button>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Dados do titular</CardTitle>
        </CardHeader>
        <CardContent className="pt-0">
          <Row label="Documento" value={a.maskedDocument} />
          <Row label="E-mail" value={a.maskedEmail} />
          <Row label="Telefone" value={a.maskedPhone} />
          <Row label="Tipo de conta" value={accountTypeLabels[a.accountType]} />
          <Row label="Criada em" value={formatDateTime(a.createdAt)} />
          {a.addresses[0] && (
            <>
              <Separator className="my-2" />
              <Row
                label="Endereço"
                value={`${a.addresses[0].street ?? ""}, ${a.addresses[0].number ?? ""} — ${a.addresses[0].city ?? ""}/${a.addresses[0].state ?? ""}`}
              />
            </>
          )}
          {a.addresses.length === 0 && (
            <Row label="Nacionalidade" value={nationalityLabels.BR} />
          )}
        </CardContent>
      </Card>

      {recent.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Últimos lançamentos</CardTitle>
          </CardHeader>
          <CardContent className="pt-0">
            {recent.map((t) => (
              <div
                key={t.transactionId}
                className="flex justify-between border-b py-2 text-sm last:border-0"
              >
                <span>
                  {txnTypeLabels[t.type]} · {t.description}
                </span>
                <Money value={t.amount} />
              </div>
            ))}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
