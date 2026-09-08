import { describe, expect, it } from "vitest";

import { maskCpf } from "./cpf";
import { maskPhone } from "./phone";
import { maskCep } from "./cep";
import { formatBRL, maskCurrency, parseBRL } from "./currency";

describe("maskCpf", () => {
  it("formats progressively and caps at 11 digits", () => {
    expect(maskCpf("123")).toBe("123");
    expect(maskCpf("1234")).toBe("123.4");
    expect(maskCpf("1234567")).toBe("123.456.7");
    expect(maskCpf("52998224725")).toBe("529.982.247-25");
    expect(maskCpf("529.982.247-25xx99")).toBe("529.982.247-25");
  });
});

describe("maskPhone", () => {
  it("handles 10 and 11 digit numbers", () => {
    expect(maskPhone("11")).toBe("(11");
    expect(maskPhone("1133224455")).toBe("(11) 3322-4455");
    expect(maskPhone("11987654321")).toBe("(11) 98765-4321");
    expect(maskPhone("")).toBe("");
  });
});

describe("maskCep", () => {
  it("adds the dash after 5 digits", () => {
    expect(maskCep("01001")).toBe("01001");
    expect(maskCep("01001000")).toBe("01001-000");
    expect(maskCep("010010001234")).toBe("01001-000");
  });
});

describe("currency", () => {
  it("maskCurrency treats trailing digits as cents", () => {
    expect(maskCurrency("")).toBe("");
    expect(maskCurrency("5")).toBe("R$ 0,05");
    expect(maskCurrency("123456")).toBe("R$ 1.234,56");
  });
  it("formatBRL and parseBRL round-trip", () => {
    expect(formatBRL(1234.56)).toBe("R$ 1.234,56");
    expect(parseBRL("R$ 1.234,56")).toBe(1234.56);
    expect(parseBRL("10,00")).toBe(10);
    expect(Number.isNaN(parseBRL("abc"))).toBe(true);
  });
});
