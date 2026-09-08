import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import AppError from "./error";

describe("AppError", () => {
  it("shows the error message and retries on click", async () => {
    const reset = vi.fn();
    render(<AppError error={new Error("deu ruim")} reset={reset} />);

    expect(screen.getByText("Algo deu errado")).toBeInTheDocument();
    expect(screen.getByText("deu ruim")).toBeInTheDocument();

    await userEvent.click(
      screen.getByRole("button", { name: "Tentar de novo" }),
    );
    expect(reset).toHaveBeenCalled();
  });

  it("shows a fallback message and the digest reference", () => {
    const error = Object.assign(new Error(""), { digest: "ref-9" });
    render(<AppError error={error} reset={vi.fn()} />);

    expect(
      screen.getByText("Erro inesperado. Tente novamente."),
    ).toBeInTheDocument();
    expect(screen.getByText("Referência: ref-9")).toBeInTheDocument();
  });
});
