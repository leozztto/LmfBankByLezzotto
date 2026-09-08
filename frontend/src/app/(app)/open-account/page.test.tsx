import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./account-form", () => ({
  AccountForm: () => <div>account-form</div>,
}));

import OpenAccountPage from "./page";

describe("OpenAccountPage", () => {
  it("renders the heading and the form", () => {
    render(<OpenAccountPage />);
    expect(
      screen.getByRole("heading", { name: "Abrir conta" }),
    ).toBeInTheDocument();
    expect(screen.getByText("account-form")).toBeInTheDocument();
  });
});
