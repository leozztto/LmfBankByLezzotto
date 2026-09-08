import { describe, expect, it } from "vitest";

import {
  accountFormSchema,
  emptyAddress,
  toAccountPayload,
  type AccountFormValues,
} from "./account";

const validValues: AccountFormValues = {
  fullName: "Maria Silva",
  documentNumber: "529.982.247-25",
  birthDate: "1990-05-20",
  motherName: "Joana Silva",
  nationality: "BR",
  email: "maria@example.com",
  phone: "(11) 98765-4321",
  profession: "Engenheira",
  monthlyIncome: "R$ 12.000,00",
  accountType: "C",
  acceptedTerms: true,
  addresses: [
    { ...emptyAddress, zipCode: "01001-000", street: "Praça da Sé", neighborhood: "Sé", number: "100", city: "São Paulo", state: "sp" },
  ],
};

describe("accountFormSchema", () => {
  it("accepts a fully valid form", () => {
    expect(accountFormSchema.safeParse(validValues).success).toBe(true);
  });

  it("rejects an invalid CPF, future birthDate, bad email and unchecked terms", () => {
    const errs = accountFormSchema.safeParse({
      ...validValues,
      documentNumber: "111.111.111-11",
      birthDate: "2999-01-01",
      email: "nope",
      acceptedTerms: false,
    });
    expect(errs.success).toBe(false);
    const paths = errs.success
      ? []
      : errs.error.issues.map((i) => i.path.join("."));
    expect(paths).toEqual(
      expect.arrayContaining([
        "documentNumber",
        "birthDate",
        "email",
        "acceptedTerms",
      ]),
    );
  });

  it("requires at least one address", () => {
    const r = accountFormSchema.safeParse({ ...validValues, addresses: [] });
    expect(r.success).toBe(false);
  });
});

describe("toAccountPayload", () => {
  it("strips masks, parses money, upper-cases UF and omits server-managed fields", () => {
    const payload = toAccountPayload(validValues);
    expect(payload).toMatchObject({
      documentNumber: "52998224725",
      phone: "11987654321",
      monthlyIncome: 12000,
      accountType: "C",
      acceptedTerms: true,
    });
    expect(payload.addresses[0]).toMatchObject({
      zipCode: "01001000",
      state: "SP",
      country: "BR",
    });
    expect(payload).not.toHaveProperty("createdAt");
    expect(payload).not.toHaveProperty("agency");
    expect(payload).not.toHaveProperty("accountStatus");
    expect(payload).not.toHaveProperty("accountNumber");
  });
});
