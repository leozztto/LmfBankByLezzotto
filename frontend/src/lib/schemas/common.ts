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

/** "1.234,56" or "1234.56" -> "1234.56" as a non-negative number string. */
export const money = z
  .string()
  .min(1, "Informe um valor")
  .transform((s) => s.replace(/\s|R\$/g, "").replace(/\./g, "").replace(",", "."))
  .pipe(z.coerce.number().nonnegative("Valor não pode ser negativo"));
