import { describe, expect, it } from "vitest";

import { cep, cpf, isValidCpf, money, pastDate, phoneBR } from "./common";

describe("isValidCpf", () => {
  it("accepts a CPF with correct check digits", () => {
    expect(isValidCpf("529.982.247-25")).toBe(true);
    expect(isValidCpf("52998224725")).toBe(true);
  });

  it("rejects wrong length, repeated digits and bad check digits", () => {
    expect(isValidCpf("123")).toBe(false);
    expect(isValidCpf("111.111.111-11")).toBe(false);
    expect(isValidCpf("52998224724")).toBe(false);
  });
});

describe("cpf schema", () => {
  it("strips punctuation and returns 11 digits", () => {
    expect(cpf.parse("529.982.247-25")).toBe("52998224725");
  });
  it("rejects invalid CPF", () => {
    expect(() => cpf.parse("11111111111")).toThrow();
  });
});

describe("phoneBR / cep", () => {
  it("phone accepts 10 or 11 digits", () => {
    expect(phoneBR.parse("(11) 98765-4321")).toBe("11987654321");
    expect(phoneBR.parse("1133224455")).toBe("1133224455");
    expect(() => phoneBR.parse("123")).toThrow();
  });
  it("cep needs 8 digits", () => {
    expect(cep.parse("01001-000")).toBe("01001000");
    expect(() => cep.parse("0100100")).toThrow();
  });
});

describe("pastDate", () => {
  it("accepts a past date and rejects a future one", () => {
    expect(pastDate.parse("1990-01-01")).toBe("1990-01-01");
    expect(() => pastDate.parse("2999-01-01")).toThrow();
    expect(() => pastDate.parse("01/01/1990")).toThrow();
  });
});

describe("money", () => {
  it("parses BR-formatted currency to a number", () => {
    expect(money.parse("1.234,56")).toBe(1234.56);
    expect(money.parse("R$ 10,00")).toBe(10);
    expect(money.parse("0")).toBe(0);
  });
  it("rejects negatives and empty", () => {
    expect(() => money.parse("-5")).toThrow();
    expect(() => money.parse("")).toThrow();
  });
});
