import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

let hasCookie = false;
const redirect = vi.fn();

vi.mock("next/headers", () => ({
  cookies: () => ({ has: () => hasCookie }),
}));
vi.mock("next/navigation", () => ({ redirect: (to: string) => redirect(to) }));

import AuthLayout from "./layout";

describe("AuthLayout", () => {
  beforeEach(() => {
    hasCookie = false;
    redirect.mockClear();
  });

  it("renders children when there is no session cookie", () => {
    render(<AuthLayout>{<div>auth child</div>}</AuthLayout>);
    expect(screen.getByText("auth child")).toBeInTheDocument();
    expect(redirect).not.toHaveBeenCalled();
  });

  it("redirects to /dashboard when a session cookie is present", () => {
    hasCookie = true;
    render(<AuthLayout>{<div>auth child</div>}</AuthLayout>);
    expect(redirect).toHaveBeenCalledWith("/dashboard");
  });
});
