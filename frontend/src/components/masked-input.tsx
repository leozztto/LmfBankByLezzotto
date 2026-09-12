"use client";

import * as React from "react";

import { Input } from "@/components/ui/input";

type InputProps = React.ComponentProps<typeof Input>;

interface MaskedInputProps extends Omit<InputProps, "onChange" | "value"> {
  /** Formats the raw string for display; also what gets stored in the form. */
  mask: (value: string) => string;
  value?: string;
  onChange?: (value: string) => void;
}

/**
 * A text input that formats its value with `mask` on every change and reports
 * the masked string back through `onChange` — so the form holds a single
 * masked value and the Zod schema normalises it for the payload.
 */
export const MaskedInput = React.forwardRef<HTMLInputElement, MaskedInputProps>(
  ({ mask, value = "", onChange, ...props }, ref) => {
    return (
      <Input
        ref={ref}
        value={mask(value)}
        onChange={(e) => onChange?.(mask(e.target.value))}
        inputMode="numeric"
        {...props}
      />
    );
  },
);
MaskedInput.displayName = "MaskedInput";
