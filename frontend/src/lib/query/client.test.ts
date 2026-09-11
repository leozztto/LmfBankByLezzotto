import { afterEach, describe, expect, it, vi } from "vitest";

import { ApiError, ValidationError } from "@/lib/api/errors";
import { notify } from "@/lib/notify";
import { useAuthStore } from "@/stores/auth-store";
import { handleGlobalError } from "./client";

vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

describe("handleGlobalError", () => {
  afterEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({ session: null });
    // @ts-expect-error jsdom location is writable enough for the test
    delete window.location;
    // @ts-expect-error minimal stub
    window.location = { href: "" };
  });

  it("ignores ValidationError (handled by the form)", () => {
    handleGlobalError(new ValidationError("x", { a: "b" }));
    expect(notify.error).not.toHaveBeenCalled();
  });

  it("on 401 clears the session and redirects to /login", () => {
    useAuthStore.setState({ session: { authenticated: true, username: "u" } });
    handleGlobalError(new ApiError(401, "UNAUTHORIZED", "no"));
    expect(useAuthStore.getState().session).toBeNull();
    expect(window.location.href).toBe("/login");
  });

  it("toasts the message for any other error", () => {
    handleGlobalError(new ApiError(500, "INTERNAL_ERROR", "explodiu"));
    expect(notify.error).toHaveBeenCalledWith("explodiu");
  });

  it("toasts a generic message for a non-Error value", () => {
    handleGlobalError("weird");
    expect(notify.error).toHaveBeenCalledWith("Erro inesperado");
  });

  it("skips everything (even the 401 redirect) when the mutation opts out", () => {
    useAuthStore.setState({ session: { authenticated: true, username: "u" } });
    handleGlobalError(new ApiError(401, "INVALID_CREDENTIALS", "no"), {
      options: { meta: { skipGlobalErrorHandler: true } },
    });
    expect(useAuthStore.getState().session).not.toBeNull();
    expect(window.location.href).toBe("");
    expect(notify.error).not.toHaveBeenCalled();
  });
});
