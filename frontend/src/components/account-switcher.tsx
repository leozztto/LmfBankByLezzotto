"use client";

import { useRouter } from "next/navigation";
import { ChevronsUpDown } from "lucide-react";

import { useAccountsQuery } from "@/hooks/use-accounts";
import { useUiStore } from "@/stores/ui-store";
import { accountTypeLabels } from "@/lib/enums";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

export function AccountSwitcher() {
  const router = useRouter();
  const { data: accounts } = useAccountsQuery();
  const selectedId = useUiStore((s) => s.selectedAccountId);
  const setSelected = useUiStore((s) => s.setSelectedAccount);

  const selected = accounts?.find((a) => a.accountId === selectedId);
  const label = selected
    ? `${selected.accountNumber} · ${selected.fullName}`
    : "Selecionar conta";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="max-w-[16rem]">
          <span className="truncate">{label}</span>
          <ChevronsUpDown className="opacity-50" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel>Contas</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {(accounts ?? []).length === 0 && (
          <DropdownMenuItem disabled>Nenhuma conta</DropdownMenuItem>
        )}
        {(accounts ?? []).map((a) => (
          <DropdownMenuItem
            key={a.accountId}
            onSelect={() => {
              setSelected(a.accountId);
              router.push(`/accounts/${a.accountId}`);
            }}
          >
            <div className="flex flex-col">
              <span className="text-sm">{a.accountNumber}</span>
              <span className="text-xs text-muted-foreground">
                {a.fullName} · {accountTypeLabels[a.accountType]}
              </span>
            </div>
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
