import { describe, expect, it } from "vitest";

import {
  ApiError,
  TransferFailedError,
  ValidationError,
  apiErrorFromBody,
} from "./errors";

describe("apiErrorFromBody", () => {
  it("builds a ValidationError when code is VALIDATION_ERROR", () => {
    const err = apiErrorFromBody(400, {
      code: "VALIDATION_ERROR",
      message: "dados inválidos",
      path: "/accounts",
      fieldErrors: { fullName: "obrigatório" },
    });

    expect(err).toBeInstanceOf(ValidationError);
    expect(err.status).toBe(400);
    expect(err.fieldErrors).toEqual({ fullName: "obrigatório" });
    expect(err.path).toBe("/accounts");
  });

  it("builds a ValidationError when only fieldErrors is present (no code)", () => {
    const err = apiErrorFromBody(400, {
      fieldErrors: { email: "inválido" },
    });

    expect(err).toBeInstanceOf(ValidationError);
    expect(err.message).toBe("Erro inesperado");
  });

  it("defaults fieldErrors to {} when the VALIDATION body omits them", () => {
    const err = apiErrorFromBody(400, { code: "VALIDATION_ERROR" });

    expect(err).toBeInstanceOf(ValidationError);
    expect(err.fieldErrors).toEqual({});
  });

  it("falls back to code UNKNOWN and a generic message for other errors", () => {
    const err = apiErrorFromBody(500, {});

    expect(err).toBeInstanceOf(ApiError);
    expect(err).not.toBeInstanceOf(ValidationError);
    expect(err.code).toBe("UNKNOWN");
    expect(err.message).toBe("Erro inesperado");
    expect(err.status).toBe(500);
  });

  it("keeps the backend code and message when provided", () => {
    const err = apiErrorFromBody(409, {
      code: "ACCOUNT_ALREADY_EXISTS",
      message: "conta já existe",
      path: "/accounts",
    });

    expect(err.code).toBe("ACCOUNT_ALREADY_EXISTS");
    expect(err.message).toBe("conta já existe");
    expect(err.path).toBe("/accounts");
  });
});

describe("TransferFailedError", () => {
  it("uses the reason when given", () => {
    const err = new TransferFailedError("conta bloqueada");
    expect(err.status).toBe(422);
    expect(err.code).toBe("TRANSFER_FAILED");
    expect(err.message).toBe("conta bloqueada");
  });

  it("falls back to a default message when the reason is null", () => {
    expect(new TransferFailedError(null).message).toBe(
      "A transferência falhou",
    );
  });
});

describe("ValidationError", () => {
  it("is a 400 VALIDATION_ERROR carrying the field errors", () => {
    const err = new ValidationError("v", { cpf: "inválido" }, "/x");
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(400);
    expect(err.code).toBe("VALIDATION_ERROR");
    expect(err.name).toBe("ValidationError");
    expect(err.fieldErrors).toEqual({ cpf: "inválido" });
  });
});
