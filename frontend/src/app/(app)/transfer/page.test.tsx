import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./transfer-form", () => ({
  TransferForm: () => <div>transfer-form</div>,
}));

import TransferPage from "./page";

describe("TransferPage", () => {
  it("renders the heading and the form", () => {
    render(<TransferPage />);
    expect(
      screen.getByRole("heading", { name: "Transferência" }),
    ).toBeInTheDocument();
    expect(screen.getByText("transfer-form")).toBeInTheDocument();
  });
});
