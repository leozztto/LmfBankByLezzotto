import { onlyDigits } from "@/lib/schemas/common";

/** `00000-000`, tolerant to partial input. */
export function maskCep(value: string): string {
  const d = onlyDigits(value).slice(0, 8);
  return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
}
