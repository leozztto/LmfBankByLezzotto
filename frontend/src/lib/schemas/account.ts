import { z } from "zod";

import { isValidCpf, onlyDigits, parseMoney } from "@/lib/schemas/common";
import { zAccountType, zAddressType, zNationality } from "@/lib/enums";

/**
 * FORM schema — validates what the user types (masked strings). No transforms,
 * so react-hook-form's input and output types stay identical. Normalisation to
 * the wire payload happens in `toAccountPayload`.
 */

export const addressFormSchema = z.object({
  zipCode: z
    .string()
    .refine((v) => onlyDigits(v).length === 8, "CEP deve ter 8 dígitos"),
  street: z.string().min(1, "Informe o logradouro"),
  neighborhood: z.string().min(1, "Informe o bairro"),
  number: z.string().min(1, "Informe o número"),
  complement: z.string().optional().default(""),
  city: z.string().min(1, "Informe a cidade"),
  state: z
    .string()
    .min(2, "UF deve ter 2 letras")
    .max(2, "UF deve ter 2 letras"),
  country: z.string().min(2).default("BR"),
  addressType: zAddressType,
});

export const accountFormSchema = z.object({
  fullName: z.string().min(3, "Informe o nome completo"),
  documentNumber: z
    .string()
    .refine((v) => onlyDigits(v).length === 11, "CPF deve ter 11 dígitos")
    .refine((v) => isValidCpf(v), "CPF inválido"),
  birthDate: z
    .string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, "Data inválida")
    .refine((v) => new Date(v) < new Date(), "Data deve ser no passado"),
  motherName: z.string().min(3, "Informe o nome da mãe"),
  nationality: zNationality,
  email: z.string().email("E-mail inválido"),
  phone: z
    .string()
    .refine(
      (v) => [10, 11].includes(onlyDigits(v).length),
      "Telefone inválido",
    ),
  profession: z.string().min(2, "Informe a profissão"),
  monthlyIncome: z
    .string()
    .min(1, "Informe a renda")
    .refine((v) => {
      const n = parseMoney(v);
      return Number.isFinite(n) && n >= 0;
    }, "Renda inválida"),
  accountType: zAccountType,
  acceptedTerms: z.boolean().refine((v) => v, "É necessário aceitar os termos"),
  addresses: z.array(addressFormSchema).min(1, "Informe ao menos um endereço"),
});

export type AccountFormValues = z.infer<typeof accountFormSchema>;
export type AddressFormValues = z.infer<typeof addressFormSchema>;

/** The exact JSON body of POST /accounts (post-Fase 3.0). */
export interface AccountCreatePayload {
  fullName: string;
  documentNumber: string;
  birthDate: string;
  motherName: string;
  nationality: string;
  email: string;
  phone: string;
  profession: string;
  monthlyIncome: number;
  accountType: string;
  acceptedTerms: boolean;
  addresses: Array<{
    zipCode: string;
    street: string;
    neighborhood: string;
    number: string;
    complement: string;
    city: string;
    state: string;
    country: string;
    addressType: string;
  }>;
}

export function toAccountPayload(v: AccountFormValues): AccountCreatePayload {
  return {
    fullName: v.fullName.trim(),
    documentNumber: onlyDigits(v.documentNumber),
    birthDate: v.birthDate,
    motherName: v.motherName.trim(),
    nationality: v.nationality,
    email: v.email.trim(),
    phone: onlyDigits(v.phone),
    profession: v.profession.trim(),
    monthlyIncome: parseMoney(v.monthlyIncome),
    accountType: v.accountType,
    acceptedTerms: v.acceptedTerms,
    addresses: v.addresses.map((a) => ({
      zipCode: onlyDigits(a.zipCode),
      street: a.street.trim(),
      neighborhood: a.neighborhood.trim(),
      number: a.number.trim(),
      complement: (a.complement ?? "").trim(),
      city: a.city.trim(),
      state: a.state.toUpperCase(),
      country: (a.country || "BR").toUpperCase(),
      addressType: a.addressType,
    })),
  };
}

export const emptyAddress: AddressFormValues = {
  zipCode: "",
  street: "",
  neighborhood: "",
  number: "",
  complement: "",
  city: "",
  state: "",
  country: "BR",
  addressType: "R",
};
