"use client";

import { useAccountsQuery } from "@/hooks/use-accounts";
import { cn } from "@/lib/utils";

/**
 * Account picker for the movement / transfer / statement forms. A native
 * `<select>` on purpose — it is fully keyboard/screen-reader friendly and works
 * everywhere (including tests) without a portal.
 */
export function AccountSelect({
  value,
  onChange,
  placeholder = "Selecione a conta",
  exclude,
  "aria-label": ariaLabel,
  id,
  className,
}: {
  value: number | null;
  onChange: (id: number) => void;
  placeholder?: string;
  exclude?: number | null;
  "aria-label"?: string;
  id?: string;
  className?: string;
}) {
  const { data: accounts } = useAccountsQuery();
  const options = (accounts ?? []).filter((a) => a.accountId !== exclude);

  return (
    <select
      id={id}
      aria-label={ariaLabel}
      value={value ?? ""}
      onChange={(e) => onChange(Number(e.target.value))}
      className={cn(
        "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50",
        className,
      )}
    >
      <option value="" disabled>
        {placeholder}
      </option>
      {options.map((a) => (
        <option key={a.accountId} value={a.accountId}>
          {a.accountNumber} · {a.fullName}
        </option>
      ))}
    </select>
  );
}
