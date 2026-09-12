import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import GlobalError from "./global-error";

describe("GlobalError", () => {
  it("shows a generic message with no digest and calls reset", async () => {
    const reset = vi.fn();
    render(<GlobalError error={new Error("boom")} reset={reset} />);

    expect(screen.getByText("Algo deu muito errado")).toBeInTheDocument();
    expect(screen.getByText("Erro inesperado.")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Tentar de novo" }),
    );
    expect(reset).toHaveBeenCalled();
  });

  it("shows the digest reference when present", () => {
    const error = Object.assign(new Error("boom"), { digest: "abc123" });
    render(<GlobalError error={error} reset={vi.fn()} />);
    expect(screen.getByText("Referência: abc123")).toBeInTheDocument();
  });
});
