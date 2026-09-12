import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./movement-form", () => ({
  MovementForm: () => <div>movement-form</div>,
}));

import DepositWithdrawPage from "./page";

describe("DepositWithdrawPage", () => {
  it("renders the heading and the form", () => {
    render(<DepositWithdrawPage />);
    expect(
      screen.getByRole("heading", { name: "Depósito / Saque" }),
    ).toBeInTheDocument();
    expect(screen.getByText("movement-form")).toBeInTheDocument();
  });
});
