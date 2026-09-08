import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./accounts-table", () => ({
  AccountsTable: () => <div>table</div>,
}));
vi.mock("./document-search", () => ({
  DocumentSearch: () => <div>search</div>,
}));

import AccountsPage from "./page";

describe("AccountsPage", () => {
  it("renders the heading, the search and the table", () => {
    render(<AccountsPage />);
    expect(screen.getByRole("heading", { name: "Contas" })).toBeInTheDocument();
    expect(screen.getByText("search")).toBeInTheDocument();
    expect(screen.getByText("table")).toBeInTheDocument();
  });
});
