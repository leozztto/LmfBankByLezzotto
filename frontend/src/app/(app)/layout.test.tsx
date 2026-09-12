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
vi.mock("@/components/app-shell", () => ({
  AppShell: ({
    session,
    children,
  }: {
    session: { username?: string };
    children: React.ReactNode;
  }) => (
    <div>
      <span>shell:{session.username ?? "anon"}</span>
      {children}
    </div>
  ),
}));

function jwt(payload: Record<string, unknown>) {
  const b64 = (o: unknown) =>
    Buffer.from(JSON.stringify(o)).toString("base64url");
  return `${b64({ alg: "HS256" })}.${b64(payload)}.sig`;
}

import AppLayout from "./layout";

describe("AppLayout", () => {
  beforeEach(() => {
    token = undefined;
    redirect.mockClear();
    // the redirect mock throws (like the real one) — silence the boundary log
    vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    vi.mocked(console.error).mockRestore?.();
  });

  it("redirects to /login when there is no token", () => {
    expect(() => render(<AppLayout>{<div>child</div>}</AppLayout>)).toThrow(
      "REDIRECT",
    );
    expect(redirect).toHaveBeenCalledWith("/login");
  });

  it("redirects to /login when the token is expired", () => {
    token = jwt({ sub: "x", exp: Math.floor(Date.now() / 1000) - 10 });
    expect(() => render(<AppLayout>{<div>child</div>}</AppLayout>)).toThrow(
      "REDIRECT",
    );
    expect(redirect).toHaveBeenCalledWith("/login");
  });

  it("renders the shell with the username for a valid token", () => {
    token = jwt({ sub: "carol", exp: Math.floor(Date.now() / 1000) + 3600 });
    render(<AppLayout>{<div>child</div>}</AppLayout>);
    expect(screen.getByText("shell:carol")).toBeInTheDocument();
    expect(screen.getByText("child")).toBeInTheDocument();
    expect(redirect).not.toHaveBeenCalled();
  });

  it("renders the shell for a token without an exp claim", () => {
    token = jwt({ sub: "dave" });
    render(<AppLayout>{<div>child</div>}</AppLayout>);
    expect(screen.getByText("shell:dave")).toBeInTheDocument();
    expect(redirect).not.toHaveBeenCalled();
  });
});
