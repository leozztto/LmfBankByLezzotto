import { describe, expect, it, vi } from "vitest";

import { ApiError, ValidationError } from "@/lib/api/errors";
import { applyFieldErrors } from "./forms";

describe("applyFieldErrors", () => {
  it("returns false for a non-ValidationError", () => {
    const setError = vi.fn();
    expect(applyFieldErrors(new ApiError(409, "X", "x"), setError)).toBe(false);
    expect(setError).not.toHaveBeenCalled();
  });

  it("pushes each fieldError and converts Spring array paths to RHF paths", () => {
    const setError = vi.fn();
    const err = new ValidationError("v", {
      fullName: "não pode ser vazio",
      "addresses[0].street": "informe o logradouro",
    });

    expect(applyFieldErrors(err, setError)).toBe(true);
    expect(setError).toHaveBeenCalledWith("fullName", {
      type: "server",
      message: "não pode ser vazio",
    });
    expect(setError).toHaveBeenCalledWith("addresses.0.street", {
      type: "server",
      message: "informe o logradouro",
    });
  });
});
