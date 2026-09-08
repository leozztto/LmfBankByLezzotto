import Link from "next/link";

import { Button } from "@/components/ui/button";

/**
 * Global 404 — reached for any URL that resolves to no route (the middleware
 * lets these through so Next can render this page inside the root layout).
 */
export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-5xl font-semibold">404</p>
      <p className="text-muted-foreground">Esta página não existe.</p>
      <Button asChild>
        <Link href="/dashboard">Ir para o painel</Link>
      </Button>
    </div>
  );
}
