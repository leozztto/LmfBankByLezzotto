import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./dashboard-summary", () => ({
  DashboardSummary: () => <div>summary</div>,
}));

import DashboardPage from "./page";

describe("DashboardPage", () => {
  it("renders the heading and the summary", () => {
    render(<DashboardPage />);
    expect(screen.getByRole("heading", { name: "Painel" })).toBeInTheDocument();
    expect(screen.getByText("summary")).toBeInTheDocument();
  });
});
