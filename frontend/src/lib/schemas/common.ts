import { z } from "zod";

export const onlyDigits = (s: string) => s.replace(/\D/g, "");

/** Validates the two CPF check digits over 11 raw digits. */
export function isValidCpf(raw: string): boolean {
  const cpf = onlyDigits(raw);
  if (cpf.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(cpf)) return false;

  const digit = (sliceLen: number) => {
    let sum = 0;
    for (let i = 0; i < sliceLen; i++) {
      sum += Number(cpf[i]) * (sliceLen + 1 - i);
    }
    const mod = (sum * 10) % 11;
    return mod === 10 ? 0 : mod;
  };

  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10]);
}

export const cpf = z
  .string()
  .transform(onlyDigits)
  .refine((v) => v.length === 11, "CPF deve ter 11 dígitos")
  .refine(isValidCpf, "CPF inválido");

export const phoneBR = z
  .string()
  .transform(onlyDigits)
  .refine((v) => v.length === 10 || v.length === 11, "Telefone inválido");

export const cep = z
  .string()
  .transform(onlyDigits)
  .refine((v) => v.length === 8, "CEP deve ter 8 dígitos");

export const pastDate = z
  .string()
  .regex(/^\d{4}-\d{2}-\d{2}$/, "Data inválida")
  .refine((v) => new Date(v) < new Date(), "Data deve ser no passado");

/** "R$ 1.234,56" / "1.234,56" / "1234.56" -> 1234.56 (NaN if unparseable). */
export function parseMoney(value: string): number {
  const normalized = value
    .replace(/\s|R\$/g, "")
    .replace(/\./g, "")
    .replace(",", ".");
  const n = Number(normalized);
  return Number.isFinite(n) ? n : NaN;
}

/** Zod field for a BR-formatted currency string -> non-negative number. */
export const money = z
  .string()
  .min(1, "Informe um valor")
  .transform(parseMoney)
  .refine((n) => Number.isFinite(n) && n >= 0, "Valor inválido");
