import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { FAKE_JWT } from "@/test/msw/handlers";

const cookieStore = { set: vi.fn() };
vi.mock("next/headers", () => ({ cookies: () => cookieStore }));

async function callLogin(body: unknown) {
  const { POST } = await import("./route");
  return POST(
    new Request("http://localhost/api/auth/login", {
      method: "POST",
      body: JSON.stringify(body),
      headers: { "content-type": "application/json" },
    }),
  );
}

describe("POST /api/auth/login", () => {
  beforeEach(() => {
    cookieStore.set.mockClear();
    vi.resetModules();
  });

  it("sets an httpOnly cookie and returns the session (no token in body)", async () => {
    const res = await callLogin({ username: "bob", password: "x" });
    const json = await res.json();

    expect(res.status).toBe(200);
    expect(json).toMatchObject({ authenticated: true, username: "demo" });
    expect(json).not.toHaveProperty("token");

    expect(cookieStore.set).toHaveBeenCalledWith(
      "lmf_token",
      FAKE_JWT,
      expect.objectContaining({ httpOnly: true, sameSite: "lax", path: "/" }),
    );
  });

  it("returns 400 for a malformed body without setting a cookie", async () => {
    const res = await callLogin({ username: "" });
    expect(res.status).toBe(400);
    expect(cookieStore.set).not.toHaveBeenCalled();
  });

  it("passes a backend 401 straight through, no cookie", async () => {
    server.use(
      http.post("http://localhost:8080/auth/login", () =>
        HttpResponse.json(
          { status: 401, code: "UNAUTHORIZED", message: "no" },
          { status: 401 },
        ),
      ),
    );
    const res = await callLogin({ username: "bob", password: "x" });
    expect(res.status).toBe(401);
    expect(cookieStore.set).not.toHaveBeenCalled();
  });
});
