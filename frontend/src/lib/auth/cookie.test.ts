import { afterEach, describe, expect, it, vi } from "vitest";

describe("resolveMaxAge", () => {
  afterEach(() => vi.restoreAllMocks());

  it("prefers a positive expiresIn (seconds)", async () => {
    const { resolveMaxAge } = await import("./cookie");
    expect(resolveMaxAge(86400, "t", () => null)).toBe(86400);
  });

  it("falls back to the JWT exp when expiresIn is missing", async () => {
    const { resolveMaxAge } = await import("./cookie");
    const at = Date.now() + 120_000;
    expect(resolveMaxAge(undefined, "t", () => at)).toBeGreaterThan(100);
    expect(resolveMaxAge(undefined, "t", () => at)).toBeLessThanOrEqual(120);
  });

  it("defaults to 1h when neither is available", async () => {
    const { resolveMaxAge } = await import("./cookie");
    expect(resolveMaxAge(0, "t", () => null)).toBe(3600);
    expect(resolveMaxAge(undefined, "t", () => Date.now() - 1000)).toBe(3600);
  });
});

describe("sessionCookieOptions", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it("is httpOnly + lax + path / and omits maxAge when not given", async () => {
    const { sessionCookieOptions } = await import("./cookie");
    expect(sessionCookieOptions()).toMatchObject({
      httpOnly: true,
      sameSite: "lax",
      path: "/",
    });
    expect(sessionCookieOptions()).not.toHaveProperty("maxAge");
  });

  it("secure follows COOKIE_SECURE", async () => {
    vi.stubEnv("COOKIE_SECURE", "true");
    vi.resetModules();
    const { sessionCookieOptions } = await import("./cookie");
    expect(sessionCookieOptions(10)).toMatchObject({ secure: true, maxAge: 10 });
  });
});
