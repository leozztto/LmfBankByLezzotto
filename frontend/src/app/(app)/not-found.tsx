import Link from "next/link";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";

/**
 * Fallback for `notFound()` thrown inside `(app)` routes that don't ship a more
 * specific `not-found.tsx`. Renders within the app shell.
 */
export default function AppNotFound() {
  return (
    <div className="mx-auto max-w-md space-y-4 py-10">
      <EmptyState
        title="Página não encontrada"
        description="O recurso que você tentou abrir não existe ou foi removido."
      />
      <Button asChild variant="outline">
        <Link href="/dashboard">Voltar para o painel</Link>
      </Button>
    </div>
  );
}
