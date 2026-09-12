import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./login-form", () => ({ LoginForm: () => <div>login-form</div> }));

import LoginPage from "./page";

describe("LoginPage", () => {
  it("renders the card with the login form", () => {
    render(<LoginPage />);
    expect(screen.getByText("LmfBank")).toBeInTheDocument();
    expect(
      screen.getByText("Entre com seu usuário e senha"),
    ).toBeInTheDocument();
    expect(screen.getByText("login-form")).toBeInTheDocument();
  });
});
