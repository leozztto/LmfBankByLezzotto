import userEvent from "@testing-library/user-event";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { ConfirmDialog } from "./confirm-dialog";

describe("ConfirmDialog", () => {
  it("renders title/description and fires onConfirm", async () => {
    const onConfirm = vi.fn();
    const onOpenChange = vi.fn();
    render(
      <ConfirmDialog
        open
        onOpenChange={onOpenChange}
        title="Confirmar saque"
        description="Sacar R$ 50,00?"
        confirmLabel="Sacar"
        onConfirm={onConfirm}
      />,
    );

    expect(screen.getByText("Confirmar saque")).toBeInTheDocument();
    expect(screen.getByText("Sacar R$ 50,00?")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Sacar" }));
    expect(onConfirm).toHaveBeenCalled();

    await userEvent.click(screen.getByRole("button", { name: "Cancelar" }));
    expect(onOpenChange).toHaveBeenCalledWith(false);
  });

  it("disables the buttons while loading", () => {
    render(
      <ConfirmDialog
        open
        onOpenChange={vi.fn()}
        title="x"
        loading
        onConfirm={vi.fn()}
      />,
    );
    expect(screen.getByRole("button", { name: "Processando…" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Cancelar" })).toBeDisabled();
  });
});
