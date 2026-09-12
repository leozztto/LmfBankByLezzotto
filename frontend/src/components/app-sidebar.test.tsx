import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

let pathname = "/dashboard";
vi.mock("next/navigation", () => ({ usePathname: () => pathname }));

import { AppSidebar } from "./app-sidebar";

describe("AppSidebar", () => {
  it("renders every nav item", () => {
    pathname = "/dashboard";
    render(<AppSidebar />);
    for (const label of [
      "Painel",
      "Abrir conta",
      "Contas",
      "Depósito / Saque",
      "Transferência",
      "Extrato",
    ]) {
      expect(screen.getByRole("link", { name: label })).toBeInTheDocument();
    }
  });

  it("marks the current section active (including sub-routes)", () => {
    pathname = "/accounts/5";
    render(<AppSidebar />);
    expect(screen.getByRole("link", { name: "Contas" })).toHaveAttribute(
      "aria-current",
      "page",
    );
    expect(screen.getByRole("link", { name: "Painel" })).not.toHaveAttribute(
      "aria-current",
    );
  });

  it("hides the Admin item for a regular user (ADR 0010)", () => {
    pathname = "/dashboard";
    render(<AppSidebar />);
    expect(
      screen.queryByRole("link", { name: "Admin" }),
    ).not.toBeInTheDocument();
  });

  it("shows the Admin item when isAdmin is true", () => {
    pathname = "/dashboard";
    render(<AppSidebar isAdmin />);
    expect(screen.getByRole("link", { name: "Admin" })).toBeInTheDocument();
  });
});
