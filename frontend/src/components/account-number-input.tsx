"use client";

import { useEffect, useState } from "react";

import { useAccountByNumberQuery } from "@/hooks/use-accounts";
import { cn } from "@/lib/utils";

/**
 * Transfer-destination picker for a regular user (ADR 0010): they can't list every
 * account like an admin's {@link import("./account-select").AccountSelect} does, so
 * they type the destination's account number and it resolves via a cross-account,
 * unrestricted lookup (`GET /accounts/number/{n}`).
 */
export function AccountNumberInput({
  value,
  onChange,
  placeholder = "Número da conta (ex: 00000001-1)",
  "aria-label": ariaLabel,
  id,
  className,
}: {
  value: number | null;
  onChange: (id: number) => void;
  placeholder?: string;
  "aria-label"?: string;
  id?: string;
  className?: string;
}) {
  const [accountNumber, setAccountNumber] = useState("");
  const { data, isFetching, isError } = useAccountByNumberQuery(accountNumber);

  useEffect(() => {
    if (data && data.accountId !== value) {
      onChange(data.accountId);
    }
  }, [data, value, onChange]);

  return (
    <div className="space-y-1">
      <input
        id={id}
        aria-label={ariaLabel}
        value={accountNumber}
        onChange={(e) => {
          setAccountNumber(e.target.value);
          onChange(0);
        }}
        placeholder={placeholder}
        className={cn(
          "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50",
          className,
        )}
      />
      {accountNumber && isFetching && (
        <p className="text-xs text-muted-foreground">Buscando conta…</p>
      )}
      {accountNumber && isError && (
        <p className="text-xs text-destructive">Conta não encontrada</p>
      )}
      {data && (
        <p className="text-xs text-muted-foreground">
          Destino: {data.fullName}
        </p>
      )}
    </div>
  );
}
