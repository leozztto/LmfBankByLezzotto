/**
 * Geradores de dados de teste. Cada spec cria as próprias contas com CPF
 * aleatório válido, então os specs não colidem entre si nem dependem de ordem.
 */

/** Dígito verificador de CPF — mesma regra de `frontend/src/lib/schemas/common.ts`. */
function cpfCheckDigit(digits: number[], factor: number): number {
  let sum = 0;
  for (let i = 0; i < digits.length; i++) {
    sum += digits[i]! * (factor - i);
  }
  const mod = (sum * 10) % 11;
  return mod === 10 ? 0 : mod;
}

/** 11 dígitos com verificadores corretos (sem pontuação). */
export function randomCpf(): string {
  const base: number[] = Array.from({ length: 9 }, () =>
    Math.floor(Math.random() * 10),
  );
  // rejeita sequências repetidas (000..., 111...) que a validação recusa
  if (new Set(base).size === 1) return randomCpf();

  const d1 = cpfCheckDigit(base, 10);
  const d2 = cpfCheckDigit([...base, d1], 11);
  return [...base, d1, d2].join("");
}

let seq = 0;
/** Sufixo único e curto para nomes/e-mails dentro de uma execução. */
function uid(): string {
  seq += 1;
  return `${Date.now().toString(36)}${seq}`;
}

export interface AccountPayload {
  fullName: string;
  documentNumber: string;
  birthDate: string;
  motherName: string;
  nationality: "BR";
  email: string;
  phone: string;
  addresses: Array<{
    zipCode: string;
    street: string;
    neighborhood: string;
    number: string;
    complement: string;
    city: string;
    state: string;
    country: string;
    addressType: "R";
  }>;
  profession: string;
  monthlyIncome: number;
  accountType: "C";
  acceptedTerms: true;
}

/** Payload válido de `POST /accounts` (bate com `AccountDto` do backend). */
export function buildAccountPayload(
  overrides: Partial<AccountPayload> = {},
): AccountPayload {
  const tag = uid();
  return {
    fullName: `E2E Titular ${tag}`,
    documentNumber: randomCpf(),
    birthDate: "1990-05-20",
    motherName: `E2E Mãe ${tag}`,
    nationality: "BR",
    email: `e2e.${tag}@example.com`,
    phone: "11987654321",
    addresses: [
      {
        zipCode: "01001000",
        street: "Praça da Sé",
        neighborhood: "Sé",
        number: "100",
        complement: "",
        city: "São Paulo",
        state: "SP",
        country: "BR",
        addressType: "R",
      },
    ],
    profession: "Engenheira",
    monthlyIncome: 12000,
    accountType: "C",
    acceptedTerms: true,
    ...overrides,
  };
}

/** Dados para preencher o formulário de abertura de conta pela UI. */
export interface AccountFormData {
  fullName: string;
  cpf: string;
  birthDate: string;
  motherName: string;
  email: string;
  phone: string;
  profession: string;
  monthlyIncome: string;
  address: {
    zipCode: string;
    street: string;
    neighborhood: string;
    number: string;
    city: string;
    state: string;
  };
}

export function buildAccountFormData(): AccountFormData {
  const tag = uid();
  return {
    fullName: `E2E UI ${tag}`,
    cpf: randomCpf(),
    birthDate: "1990-05-20",
    motherName: `E2E Mãe UI ${tag}`,
    email: `e2e.ui.${tag}@example.com`,
    phone: "11987654321",
    profession: "Engenheira",
    monthlyIncome: "12000",
    address: {
      zipCode: "01001000",
      street: "Praça da Sé",
      neighborhood: "Sé",
      number: "100",
      city: "São Paulo",
      state: "SP",
    },
  };
}

const brlFmt = new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
});

/**
 * `1234.5` -> `"R$ 1.234,50"`. O front normaliza o NBSP que o Intl coloca entre
 * "R$" e o número para um espaço comum (`formatBRL` em currency.ts) — fazemos o
 * mesmo aqui para os seletores baterem.
 */
export function brl(value: number): string {
  return brlFmt.format(value).replace(/[  ]/g, " ");
}
