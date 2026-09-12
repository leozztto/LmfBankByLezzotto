import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import NotFound from "./not-found";

describe("global NotFound", () => {
  it("renders the 404 message and a link back to the dashboard", () => {
    render(<NotFound />);
    expect(screen.getByText("404")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Ir para o painel" }),
    ).toHaveAttribute("href", "/dashboard");
  });
});
