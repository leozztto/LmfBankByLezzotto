"use client";

import { useAccountsQuery } from "@/hooks/use-accounts";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

/**
 * Reusable account picker for the movement / transfer forms. Value is the
 * account id (as a string, since Radix Select works with strings).
 */
export function AccountSelect({
  value,
  onChange,
  placeholder = "Selecione a conta",
  exclude,
}: {
  value: number | null;
  onChange: (id: number) => void;
  placeholder?: string;
  exclude?: number | null;
}) {
  const { data: accounts } = useAccountsQuery();
  const options = (accounts ?? []).filter((a) => a.accountId !== exclude);

  return (
    <Select
      value={value ? String(value) : undefined}
      onValueChange={(v) => onChange(Number(v))}
    >
      <SelectTrigger>
        <SelectValue placeholder={placeholder} />
      </SelectTrigger>
      <SelectContent>
        {options.map((a) => (
          <SelectItem key={a.accountId} value={String(a.accountId)}>
            {a.accountNumber} · {a.fullName}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
}
