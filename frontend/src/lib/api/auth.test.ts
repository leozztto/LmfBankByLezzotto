import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { getSession, login, logout } from "./auth";

describe("auth api (hits the BFF)", () => {
  it("login posts credentials and returns the session", async () => {
    let body: unknown;
    server.use(
      http.post("/api/auth/login", async ({ request }) => {
        body = await request.json();
        return HttpResponse.json({ authenticated: true, username: "demo" });
      }),
    );
    const session = await login({ username: "demo", password: "x" });
    expect(body).toEqual({ username: "demo", password: "x" });
    expect(session).toEqual({ authenticated: true, username: "demo" });
  });

  it("logout returns not authenticated", async () => {
    server.use(
      http.post("/api/auth/logout", () =>
        HttpResponse.json({ authenticated: false }),
      ),
    );
    expect(await logout()).toEqual({ authenticated: false });
  });

  it("getSession reads the current session", async () => {
    server.use(
      http.get("/api/auth/session", () =>
        HttpResponse.json({ authenticated: true, username: "carol" }),
      ),
    );
    expect(await getSession()).toMatchObject({ username: "carol" });
  });
});
