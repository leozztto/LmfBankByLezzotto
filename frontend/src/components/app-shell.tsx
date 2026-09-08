import type { Session } from "@/lib/schemas/auth";
import { AccountSwitcher } from "@/components/account-switcher";
import { AppSidebar } from "@/components/app-sidebar";
import { LogoutButton } from "@/components/logout-button";
import { SessionBootstrap } from "@/components/session-bootstrap";

export function AppShell({
  session,
  children,
}: {
  session: Session;
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <SessionBootstrap session={session} />

      <aside className="hidden w-60 shrink-0 border-r bg-muted/20 md:block">
        <div className="flex h-14 items-center border-b px-5 font-semibold">
          LmfBank
        </div>
        <AppSidebar />
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 items-center justify-between gap-3 border-b px-5">
          <AccountSwitcher />
          <div className="flex items-center gap-3">
            <span className="hidden text-sm text-muted-foreground sm:inline">
              {session.username ?? "sessão"}
            </span>
            <LogoutButton />
          </div>
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
