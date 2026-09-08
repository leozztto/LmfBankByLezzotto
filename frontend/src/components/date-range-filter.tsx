"use client";

import { useState } from "react";

import type { DateRange } from "@/lib/api/statement";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

/**
 * Period filter for the statement. The backend only filters when BOTH dates are
 * given; `onChange(null)` clears it.
 */
export function DateRangeFilter({
  value,
  onChange,
}: {
  value: DateRange | null;
  onChange: (range: DateRange | null) => void;
}) {
  const [start, setStart] = useState(value?.start ?? "");
  const [end, setEnd] = useState(value?.end ?? "");

  const bothSet = start !== "" && end !== "";
  const invalidOrder = bothSet && start > end;

  return (
    <form
      className="flex flex-wrap items-end gap-3"
      onSubmit={(e) => {
        e.preventDefault();
        if (bothSet && !invalidOrder) onChange({ start, end });
      }}
    >
      <div className="space-y-1">
        <Label htmlFor="range-start">De</Label>
        <Input
          id="range-start"
          type="date"
          value={start}
          max={end || undefined}
          onChange={(e) => setStart(e.target.value)}
          className="w-40"
        />
      </div>
      <div className="space-y-1">
        <Label htmlFor="range-end">Até</Label>
        <Input
          id="range-end"
          type="date"
          value={end}
          min={start || undefined}
          onChange={(e) => setEnd(e.target.value)}
          className="w-40"
        />
      </div>
      <Button type="submit" variant="secondary" disabled={!bothSet || invalidOrder}>
        Aplicar
      </Button>
      {value && (
        <Button
          type="button"
          variant="ghost"
          onClick={() => {
            setStart("");
            setEnd("");
            onChange(null);
          }}
        >
          Limpar
        </Button>
      )}
      {invalidOrder && (
        <p className="w-full text-xs text-destructive">
          A data inicial deve ser anterior à final.
        </p>
      )}
    </form>
  );
}
