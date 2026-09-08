import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AccountNotFound from "./not-found";

describe("AccountNotFound", () => {
  it("renders the message and a link back to the account list", () => {
    render(<AccountNotFound />);
    expect(screen.getByText("Conta não encontrada")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Voltar para contas" }),
    ).toHaveAttribute("href", "/accounts");
  });
});
