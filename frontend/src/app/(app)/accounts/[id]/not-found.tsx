import Link from "next/link";

import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/empty-state";

export default function AccountNotFound() {
  return (
    <div className="mx-auto max-w-md space-y-4 py-10">
      <EmptyState
        title="Conta não encontrada"
        description="Verifique o número da conta ou volte para a lista."
      />
      <Button asChild variant="outline">
        <Link href="/accounts">Voltar para contas</Link>
      </Button>
    </div>
  );
}
