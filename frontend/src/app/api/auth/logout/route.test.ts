import { describe, expect, it, vi } from "vitest";

const cookieStore = { set: vi.fn() };
vi.mock("next/headers", () => ({ cookies: () => cookieStore }));

describe("POST /api/auth/logout", () => {
  it("clears the cookie (maxAge 0) and reports not authenticated", async () => {
    const { POST } = await import("./route");
    const res = await POST();

    expect(await res.json()).toEqual({ authenticated: false });
    expect(cookieStore.set).toHaveBeenCalledWith(
      "lmf_token",
      "",
      expect.objectContaining({ maxAge: 0, httpOnly: true }),
    );
  });
});
