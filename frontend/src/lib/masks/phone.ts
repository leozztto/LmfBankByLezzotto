import { onlyDigits } from "@/lib/schemas/common";

/** `(00) 0000-0000` for 10 digits, `(00) 00000-0000` for 11. Tolerant to partials. */
export function maskPhone(value: string): string {
  const d = onlyDigits(value).slice(0, 11);
  if (d.length === 0) return "";
  if (d.length <= 2) return `(${d}`;
  const ddd = d.slice(0, 2);
  const rest = d.slice(2);
  const split = rest.length > 8 ? 5 : 4;
  const head = rest.slice(0, split);
  const tail = rest.slice(split);
  return tail ? `(${ddd}) ${head}-${tail}` : `(${ddd}) ${head}`;
}
