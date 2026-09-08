import userEvent from "@testing-library/user-event";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { DateRangeFilter } from "./date-range-filter";

describe("DateRangeFilter", () => {
  it("applies only when both dates are set", async () => {
    const onChange = vi.fn();
    render(<DateRangeFilter value={null} onChange={onChange} />);

    const apply = screen.getByRole("button", { name: "Aplicar" });
    expect(apply).toBeDisabled();

    await userEvent.type(screen.getByLabelText("De"), "2026-01-01");
    expect(apply).toBeDisabled();
    await userEvent.type(screen.getByLabelText("Até"), "2026-02-01");
    expect(apply).toBeEnabled();

    await userEvent.click(apply);
    expect(onChange).toHaveBeenCalledWith({
      start: "2026-01-01",
      end: "2026-02-01",
    });
  });

  it("blocks a start date after the end date", async () => {
    const onChange = vi.fn();
    render(<DateRangeFilter value={null} onChange={onChange} />);
    await userEvent.type(screen.getByLabelText("De"), "2026-05-01");
    await userEvent.type(screen.getByLabelText("Até"), "2026-01-01");

    expect(
      screen.getByText("A data inicial deve ser anterior à final."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Aplicar" })).toBeDisabled();
  });

  it("clears the range", async () => {
    const onChange = vi.fn();
    render(
      <DateRangeFilter
        value={{ start: "2026-01-01", end: "2026-02-01" }}
        onChange={onChange}
      />,
    );
    await userEvent.click(screen.getByRole("button", { name: "Limpar" }));
    expect(onChange).toHaveBeenCalledWith(null);
  });
});
