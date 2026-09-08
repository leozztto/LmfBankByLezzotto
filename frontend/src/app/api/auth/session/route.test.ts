import { beforeEach, describe, expect, it, vi } from "vitest";

let token: string | undefined;
const cookieStore = {
  get: (name: string) =>
    name === "lmf_token" && token ? { value: token } : undefined,
  set: vi.fn(),
};
vi.mock("next/headers", () => ({ cookies: () => cookieStore }));

function jwt(payload: Record<string, unknown>) {
  const b64 = (o: unknown) =>
    Buffer.from(JSON.stringify(o)).toString("base64url");
  return `${b64({ alg: "HS256" })}.${b64(payload)}.sig`;
}

describe("GET /api/auth/session", () => {
  beforeEach(() => {
    token = undefined;
    cookieStore.set.mockClear();
  });

  it("no cookie -> not authenticated", async () => {
    const { GET } = await import("./route");
    expect(await (await GET()).json()).toEqual({ authenticated: false });
  });

  it("valid cookie -> authenticated with username", async () => {
    token = jwt({ sub: "carol", exp: Math.floor(Date.now() / 1000) + 3600 });
    const { GET } = await import("./route");
    expect(await (await GET()).json()).toMatchObject({
      authenticated: true,
      username: "carol",
    });
  });

  it("valid cookie without exp -> authenticated, expiresAt undefined", async () => {
    token = jwt({ sub: "dave" });
    const { GET } = await import("./route");
    const json = await (await GET()).json();
    expect(json).toEqual({ authenticated: true, username: "dave" });
    expect(json).not.toHaveProperty("expiresAt");
  });

  it("expired cookie -> not authenticated and cleared", async () => {
    token = jwt({ sub: "carol", exp: Math.floor(Date.now() / 1000) - 10 });
    const { GET } = await import("./route");
    expect(await (await GET()).json()).toEqual({ authenticated: false });
    expect(cookieStore.set).toHaveBeenCalledWith(
      "lmf_token",
      "",
      expect.any(Object),
    );
  });
});
