import { ApiError, ValidationError } from "@/lib/api/errors";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

/**
 * Shows a business error (409, 422, 403, 400 same-account, ...) inline.
 * Returns null for ValidationError (the form maps those onto fields) and for
 * anything that isn't an ApiError.
 */
export function ApiErrorAlert({ error }: { error: unknown }) {
  if (!(error instanceof ApiError) || error instanceof ValidationError) {
    return null;
  }

  return (
    <Alert variant="destructive">
      <AlertTitle>Não foi possível concluir</AlertTitle>
      <AlertDescription>{error.message}</AlertDescription>
    </Alert>
  );
}
