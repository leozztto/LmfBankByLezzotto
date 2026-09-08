import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useUiStore } from "@/stores/ui-store";
import { TransferForm } from "./transfer-form";

vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

const acc = (id: number) => ({
  accountId: id,
  fullName: `Conta ${id}`,
  maskedDocument: "***",
  maskedEmail: "a***@e.com",
  maskedPhone: "(11) *****-1",
  accountType: "C",
  accountNumber: `${id}00-${id}`,
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: 0, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
});

const transferBody = (over: Record<string, unknown>) => ({
  transferId: "tr1",
  fromAccountId: 1,
  toAccountId: 2,
  amount: 50,
  status: "COMPLETED",
  createdAt: "2026-09-08T10:00:00",
  failureReason: null,
  ...over,
});

async function fillAndConfirm(user: ReturnType<typeof userEvent.setup>) {
  const dest = await screen.findByRole("combobox", {
    name: "Conta de destino",
  });
  await user.selectOptions(
    dest,
    await within(dest).findByRole("option", { name: /200-2/ }),
  );
  await user.type(screen.getByLabelText("Valor"), "5000"); // R$ 50,00
  await user.click(screen.getByRole("button", { name: "Transferir" }));
  const dialog = await screen.findByRole("dialog");
  await user.click(within(dialog).getByRole("button", { name: "Transferir" }));
}

describe("TransferForm", () => {
  beforeEach(() => {
    useUiStore.setState({ selectedAccountId: 1 });
    server.use(
      http.get("/api/accounts", () => HttpResponse.json([acc(1), acc(2)])),
    );
  });

  it("shows the backend 422 (insufficient balance) inline", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/transfers", () =>
        HttpResponse.json(
          {
            status: 422,
            code: "INSUFFICIENT_BALANCE",
            message:
              "Saldo insuficiente para conta 1. Saldo atual: 10, valor solicitado: 50",
          },
          { status: 422 },
        ),
      ),
    );

    renderWithProviders(<TransferForm />);
    await fillAndConfirm(user);

    expect(
      await screen.findByText(/Saldo insuficiente para conta 1/),
    ).toBeInTheDocument();
  });

  it("treats a 201 with status FAILED as an error", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/transfers", () =>
        HttpResponse.json(
          transferBody({
            status: "FAILED",
            failureReason: "conta de destino bloqueada",
          }),
          { status: 201 },
        ),
      ),
    );

    renderWithProviders(<TransferForm />);
    await fillAndConfirm(user);

    expect(
      await screen.findByText("conta de destino bloqueada"),
    ).toBeInTheDocument();
  });

  it("succeeds and clears the amount", async () => {
    const user = userEvent.setup();
    let posted: Record<string, unknown> | undefined;
    server.use(
      http.post("/api/transfers", async ({ request }) => {
        posted = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(transferBody({ status: "COMPLETED" }), {
          status: 201,
        });
      }),
    );

    renderWithProviders(<TransferForm />);
    await fillAndConfirm(user);

    await waitFor(() => expect(screen.getByLabelText("Valor")).toHaveValue(""));
    expect(posted).toMatchObject({
      fromAccountId: 1,
      toAccountId: 2,
      amount: 50,
    });
    expect(posted).toHaveProperty("idempotencyKey");
  });

  it("defaults both account selects to the placeholder when no account is selected", async () => {
    useUiStore.setState({ selectedAccountId: null });

    renderWithProviders(<TransferForm />);

    const origem = await screen.findByRole("combobox", {
      name: "Conta de origem",
    });
    const destino = screen.getByRole("combobox", { name: "Conta de destino" });
    expect(origem).toHaveValue("");
    expect(destino).toHaveValue("");
  });

  it("blocks submit when amount is below the minimum", async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.post("/api/transfers", () => {
        called = true;
        return HttpResponse.json(transferBody({}), { status: 201 });
      }),
    );

    renderWithProviders(<TransferForm />);
    const dest = await screen.findByRole("combobox", {
      name: "Conta de destino",
    });
    await user.selectOptions(
      dest,
      await within(dest).findByRole("option", { name: /200-2/ }),
    );
    await user.type(screen.getByLabelText("Valor"), "0");
    await user.click(screen.getByRole("button", { name: "Transferir" }));

    expect(await screen.findByText("Valor mínimo R$ 0,01")).toBeInTheDocument();
    expect(called).toBe(false);
  });
});
