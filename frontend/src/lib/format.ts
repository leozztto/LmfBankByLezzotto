const dateTimeFmt = new Intl.DateTimeFormat("pt-BR", {
  dateStyle: "short",
  timeStyle: "short",
});
const dateFmt = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" });

/** ISO LocalDateTime (`2026-09-08T04:02:26.597`) -> `08/09/2026 04:02`. */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? "—" : dateTimeFmt.format(d);
}

/** `yyyy-MM-dd` -> `dd/MM/yyyy`. */
export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(`${iso}T00:00:00`);
  return Number.isNaN(d.getTime()) ? "—" : dateFmt.format(d);
}
