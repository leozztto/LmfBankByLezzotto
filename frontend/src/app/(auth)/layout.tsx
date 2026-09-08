import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { SESSION_COOKIE } from "@/lib/auth/constants";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  if (cookies().has(SESSION_COOKIE)) {
    redirect("/dashboard");
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/40 p-4">
      <div className="w-full max-w-sm">{children}</div>
    </div>
  );
}
