import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { Money } from "./money";

describe("Money", () => {
  it("formats a decimal string as BRL", () => {
    render(<Money value="1234.5" />);
    expect(screen.getByText("R$ 1.234,50")).toBeInTheDocument();
  });

  it("accepts a number", () => {
    render(<Money value={10} />);
    expect(screen.getByText("R$ 10,00")).toBeInTheDocument();
  });

  it("renders a dash for a non-numeric value", () => {
    render(<Money value="abc" />);
    expect(screen.getByText("—")).toBeInTheDocument();
  });
});
