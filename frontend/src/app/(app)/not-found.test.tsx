import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AppNotFound from "./not-found";

describe("AppNotFound", () => {
  it("renders the message and a link back to the dashboard", () => {
    render(<AppNotFound />);
    expect(screen.getByText("Página não encontrada")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Voltar para o painel" }),
    ).toHaveAttribute("href", "/dashboard");
  });
});
