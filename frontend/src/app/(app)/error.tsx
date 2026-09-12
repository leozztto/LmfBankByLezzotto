"use client";

import { Button } from "@/components/ui/button";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

export default function AppError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div className="mx-auto max-w-md space-y-4 py-10">
      <Alert variant="destructive">
        <AlertTitle>Algo deu errado</AlertTitle>
        <AlertDescription>
          {error.message || "Erro inesperado. Tente novamente."}
          {error.digest && (
            <span className="mt-1 block text-xs opacity-70">
              Referência: {error.digest}
            </span>
          )}
        </AlertDescription>
      </Alert>
      <Button onClick={reset}>Tentar de novo</Button>
    </div>
  );
}
