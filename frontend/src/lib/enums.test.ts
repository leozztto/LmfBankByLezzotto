import { describe, expect, it } from "vitest";

import {
  accountStatusLabels,
  accountTypeLabels,
  accountTypeOptions,
  addressTypeLabels,
  nationalityLabels,
  txnStatusLabels,
  txnTypeLabels,
  zAccountType,
  zNationality,
  zTxnTypeName,
} from "./enums";

describe("enum labels (PT-BR)", () => {
  it("covers every account-domain code", () => {
    expect(nationalityLabels.BR).toBe("Brasileira");
    expect(accountTypeLabels.S).toBe("Conta poupança");
    expect(accountStatusLabels.A).toBe("Ativa");
    expect(addressTypeLabels.R).toBe("Residencial");
  });

  it("maps transaction type by code AND by name", () => {
    expect(txnTypeLabels.C).toBe("Crédito");
    expect(txnTypeLabels.CREDIT).toBe("Crédito");
    expect(txnTypeLabels.D).toBe(txnTypeLabels.DEBIT);
  });

  it("maps transaction status names", () => {
    expect(txnStatusLabels.COMPLETED).toBe("Concluída");
    expect(txnStatusLabels.FAILED).toBe("Falhou");
  });

  it("builds select options", () => {
    expect(accountTypeOptions).toContainEqual({
      value: "C",
      label: "Conta corrente",
    });
  });
});

describe("wire-code zod enums", () => {
  it("accept valid codes and reject others", () => {
    expect(zNationality.parse("BR")).toBe("BR");
    expect(() => zNationality.parse("BRAZILIAN")).toThrow();
    expect(zAccountType.parse("C")).toBe("C");
    expect(zTxnTypeName.parse("DEBIT")).toBe("DEBIT");
    expect(() => zTxnTypeName.parse("D")).toThrow();
  });
});
