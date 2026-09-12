"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  ArrowLeftRight,
  FileText,
  LayoutDashboard,
  PiggyBank,
  ShieldCheck,
  UserPlus,
  Users,
} from "lucide-react";

import { cn } from "@/lib/utils";

const NAV = [
  { href: "/dashboard", label: "Painel", icon: LayoutDashboard },
  { href: "/open-account", label: "Abrir conta", icon: UserPlus },
  { href: "/accounts", label: "Contas", icon: Users },
  { href: "/deposit-withdraw", label: "Depósito / Saque", icon: PiggyBank },
  { href: "/transfer", label: "Transferência", icon: ArrowLeftRight },
  { href: "/statement", label: "Extrato", icon: FileText },
];

const ADMIN_NAV_ITEM = { href: "/admin", label: "Admin", icon: ShieldCheck };

/** `isAdmin` mostra o item "Admin" (ADR 0010) — a rota em si também se protege sozinha
 *  (`(app)/admin/page.tsx`), então esconder o link aqui é só uma questão de UX. */
export function AppSidebar({ isAdmin = false }: { isAdmin?: boolean }) {
  const pathname = usePathname();
  const items = isAdmin ? [...NAV, ADMIN_NAV_ITEM] : NAV;

  return (
    <nav className="flex flex-col gap-1 p-3">
      {items.map(({ href, label, icon: Icon }) => {
        const active = pathname === href || pathname.startsWith(`${href}/`);
        return (
          <Link
            key={href}
            href={href}
            aria-current={active ? "page" : undefined}
            className={cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              active
                ? "bg-accent text-accent-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
            )}
          >
            <Icon className="size-4" />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
