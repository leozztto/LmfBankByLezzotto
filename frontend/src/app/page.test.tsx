import { describe, expect, it, vi } from "vitest";

const redirect = vi.fn();
vi.mock("next/navigation", () => ({ redirect: (to: string) => redirect(to) }));

import RootPage from "./page";

describe("RootPage", () => {
  it("redirects to /dashboard", () => {
    RootPage();
    expect(redirect).toHaveBeenCalledWith("/dashboard");
  });
});
