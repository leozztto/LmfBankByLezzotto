import { NextRequest } from "next/server";
import { describe, expect, it } from "vitest";

import { middleware } from "./middleware";

function request(path: string, opts?: { cookie?: boolean }) {
  const req = new NextRequest(new Request(`http://localhost${path}`));
  if (opts?.cookie) req.cookies.set("lmf_token", "x");
  return req;
}

describe("middleware", () => {
  it("no cookie on a protected route -> redirect to /login?next=", () => {
    const res = middleware(request("/dashboard"));
    expect(res.status).toBe(307);
    const loc = new URL(res.headers.get("location")!);
    expect(loc.pathname).toBe("/login");
    expect(loc.searchParams.get("next")).toBe("/dashboard");
  });

  it("cookie on a protected route -> pass through", () => {
    const res = middleware(request("/accounts", { cookie: true }));
    expect(res.headers.get("location")).toBeNull();
  });

  it("cookie on /login -> redirect to /dashboard", () => {
    const res = middleware(request("/login", { cookie: true }));
    expect(new URL(res.headers.get("location")!).pathname).toBe("/dashboard");
  });

  it("no cookie on /login -> pass through", () => {
    const res = middleware(request("/login"));
    expect(res.headers.get("location")).toBeNull();
  });
});
