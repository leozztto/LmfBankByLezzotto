import { render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

let token: string | undefined;
const redirect = vi.fn((to: string) => {
  throw new Error(`REDIRECT:${to}`);
});

vi.mock("next/headers", () => ({
  cookies: () => ({
    get: (name: string) =>
      name === "lmf_token" && token ? { value: token } : undefined,
  }),
}));
vi.mock("next/navigation", () => ({ redirect: (to: string) => redirect(to) }));
// As próprias forms têm teste dedicado — aqui só interessa o guard de role.
vi.mock("./create-user-form", () => ({
  CreateUserForm: () => <div>create-user-form</div>,
}));
vi.mock("./link-account-form", () => ({
  LinkAccountForm: () => <div>link-account-form</div>,
}));

function jwt(payload: Record<string, unknown>) {
  const b64 = (o: unknown) =>
    Buffer.from(JSON.stringify(o)).toString("base64url");
  return `${b64({ alg: "HS256" })}.${b64(payload)}.sig`;
}

import AdminPage from "./page";

describe("AdminPage", () => {
  beforeEach(() => {
    token = undefined;
    redirect.mockClear();
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.mocked(console.error).mockRestore?.();
  });

  it("redirects a regular user to /dashboard (ADR 0010)", () => {
    token = jwt({ sub: "demo", role: "USER" });
    expect(() => render(<AdminPage />)).toThrow("REDIRECT");
    expect(redirect).toHaveBeenCalledWith("/dashboard");
  });

  it("redirects when there is no token at all", () => {
    expect(() => render(<AdminPage />)).toThrow("REDIRECT");
    expect(redirect).toHaveBeenCalledWith("/dashboard");
  });

  it("renders the forms for an admin", () => {
    token = jwt({ sub: "admin", role: "ADMIN" });
    render(<AdminPage />);
    expect(redirect).not.toHaveBeenCalled();
    expect(
      screen.getByRole("heading", { name: "Criar usuário" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Vincular conta" }),
    ).toBeInTheDocument();
  });
});
