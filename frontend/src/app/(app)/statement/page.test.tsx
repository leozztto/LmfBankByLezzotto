import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./statement-table", () => ({
  StatementTable: () => <div>statement-table</div>,
}));

import StatementPage from "./page";

describe("StatementPage", () => {
  it("renders the heading and the table", () => {
    render(<StatementPage />);
    expect(
      screen.getByRole("heading", { name: "Extrato" }),
    ).toBeInTheDocument();
    expect(screen.getByText("statement-table")).toBeInTheDocument();
  });
});
