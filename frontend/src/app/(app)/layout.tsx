import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { SESSION_COOKIE } from "@/lib/auth/constants";
import { decodeJwt, isExpired } from "@/lib/auth/jwt";
import type { Session } from "@/lib/schemas/auth";
import { AppShell } from "@/components/app-shell";

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const token = cookies().get(SESSION_COOKIE)?.value;

  if (!token || isExpired(token)) {
    redirect("/login");
  }

  const claims = decodeJwt(token);
  const session: Session = {
    authenticated: true,
    username: claims?.sub,
    role: claims?.role === "ADMIN" ? "ADMIN" : "USER",
    expiresAt: typeof claims?.exp === "number" ? claims.exp * 1000 : undefined,
  };

  return <AppShell session={session}>{children}</AppShell>;
}
