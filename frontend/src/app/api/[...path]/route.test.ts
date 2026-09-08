import { http, HttpResponse } from "msw";
import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";

let token: string | undefined = "the-jwt";
vi.mock("next/headers", () => ({
  cookies: () => ({
    get: (n: string) => (n === "lmf_token" && token ? { value: token } : undefined),
  }),
}));

function req(url: string, init?: RequestInit) {
  return new NextRequest(new Request(url, init));
}

describe("BFF catch-all proxy /api/[...path]", () => {
  beforeEach(() => {
    token = "the-jwt";
  });

  it("forwards GET to the backend with the Bearer header, keeping the query string", async () => {
    let seen: Request | undefined;
    server.use(
      http.get("http://localhost:8080/accounts/statement", ({ request }) => {
        seen = request;
        return HttpResponse.json({ ok: true });
      }),
    );

    const { GET } = await import("./route");
    const res = await GET(
      req("http://localhost/api/accounts/statement?accountId=1"),
      { params: { path: ["accounts", "statement"] } },
    );

    expect(res.status).toBe(200);
    expect(new URL(seen!.url).search).toBe("?accountId=1");
    expect(seen!.headers.get("authorization")).toBe("Bearer the-jwt");
    expect(seen!.headers.get("cookie")).toBeNull();
  });

  it("forwards a POST body and passes the backend status + ApiError through", async () => {
    let body: unknown;
    server.use(
      http.post("http://localhost:8080/transfers", async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(
          { status: 422, code: "INSUFFICIENT_BALANCE", message: "no" },
          { status: 422 },
        );
      }),
    );

    const { POST } = await import("./route");
    const res = await POST(
      req("http://localhost/api/transfers", {
        method: "POST",
        body: JSON.stringify({ amount: 10 }),
        headers: { "content-type": "application/json" },
      }),
      { params: { path: ["transfers"] } },
    );

    expect(res.status).toBe(422);
    expect(await res.json()).toMatchObject({ code: "INSUFFICIENT_BALANCE" });
    expect(body).toEqual({ amount: 10 });
  });

  it("omits the Authorization header when there is no cookie", async () => {
    token = undefined;
    let auth: string | null = "x";
    server.use(
      http.get("http://localhost:8080/accounts", ({ request }) => {
        auth = request.headers.get("authorization");
        return HttpResponse.json([]);
      }),
    );

    const { GET } = await import("./route");
    await GET(req("http://localhost/api/accounts"), {
      params: { path: ["accounts"] },
    });
    expect(auth).toBeNull();
  });
});
