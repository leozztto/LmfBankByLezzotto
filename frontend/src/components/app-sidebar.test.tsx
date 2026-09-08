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
});
