import type { Session } from "@/lib/schemas/auth";
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
        <header className="flex h-14 items-center justify-between border-b px-5">
          <span className="text-sm text-muted-foreground">
            {session.username ?? "sessão"}
          </span>
          <LogoutButton />
        </header>
        <main className="flex-1 p-6">{children}</main>
      </div>
    </div>
  );
}
