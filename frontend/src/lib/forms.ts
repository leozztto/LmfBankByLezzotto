import type { FieldValues, Path, UseFormSetError } from "react-hook-form";

import { ValidationError } from "@/lib/api/errors";

/** `addresses[0].street` (Spring) -> `addresses.0.street` (react-hook-form). */
function toRhfPath(field: string): string {
  return field.replace(/\[(\d+)\]/g, ".$1");
}

/**
 * If `error` is a ValidationError, pushes each backend fieldError onto the
 * matching form field and returns true. Otherwise returns false so the caller
 * can show it inline.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
): boolean {
  if (!(error instanceof ValidationError)) return false;

  for (const [field, message] of Object.entries(error.fieldErrors ?? {})) {
    setError(toRhfPath(field) as Path<T>, { type: "server", message });
  }
  return true;
}
