const brl = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

// Intl separates "R$" from the number with a NBSP (or narrow NBSP on some ICU
// builds); normalise to a plain space so output is predictable.
const norm = (s: string) => s.replace(/[  ]/g, " ");

/** Number -> "R$ 1.234,56". */
export function formatBRL(value: number): string {
  return norm(brl.format(value));
}

/**
 * Formats digits typed into an input as BRL, treating the last two as cents.
 * "" -> ""; "5" -> "R$ 0,05"; "123456" -> "R$ 1.234,56".
 */
export function maskCurrency(value: string): string {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  return norm(brl.format(Number(digits) / 100));
}

/** "R$ 1.234,56" or "1.234,56" -> 1234.56 */
export function parseBRL(value: string): number {
  const normalized = value
    .replace(/[\s  ]|R\$/g, "")
    .replace(/\./g, "")
    .replace(",", ".");
  const n = Number(normalized);
  return Number.isFinite(n) ? n : NaN;
}
