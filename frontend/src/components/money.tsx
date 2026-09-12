import { formatBRL } from "@/lib/masks/currency";
import { cn } from "@/lib/utils";

/** Renders a decimal string/number as BRL. Backend sends money as strings. */
export function Money({
  value,
  className,
}: {
  value: string | number;
  className?: string;
}) {
  const n = typeof value === "number" ? value : Number(value);
  return (
    <span className={cn("tabular-nums", className)}>
      {Number.isFinite(n) ? formatBRL(n) : "—"}
    </span>
  );
}
