import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useUiStore } from "@/stores/ui-store";
import { notify } from "@/lib/notify";
import { MovementForm } from "./movement-form";

vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

const acc = (id: number) => ({
  accountId: id,
  fullName: "Ana",
  maskedDocument: "***",
  maskedEmail: "a***@e.com",
  maskedPhone: "(11) *****-1",
  accountType: "C",
  accountNumber: `${id}-2`,
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: 0, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
});

const statement = (balance: number) => ({
  accountId: 1,
  balance,
  startDate: null,
  endDate: null,
  transactions: [],
});

async function fill(user: ReturnType<typeof userEvent.setup>, amount: string) {
  await user.type(screen.getByLabelText("Valor"), amount);
  await user.type(screen.getByLabelText("Descrição"), "teste");
}

describe("MovementForm", () => {
  beforeEach(() => {
    useUiStore.setState({ selectedAccountId: 1 });
    server.use(
      http.get("/api/accounts", () => HttpResponse.json([acc(1)])),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json(statement(20)),
      ),
    );
  });

  it("deposits: confirm dialog -> POST -> success invalidates the statement", async () => {
    const user = userEvent.setup();
    let posted: Record<string, unknown> | undefined;
    server.use(
      http.post("/api/transactions", async ({ request }) => {
        posted = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          {
            transactionId: "t1",
            accountId: 1,
            type: "CREDIT",
            amount: 100,
            status: "COMPLETED",
            description: "teste",
            createdAt: "2026-09-08T10:00:00",
            transferId: null,
          },
          { status: 201 },
        );
      }),
    );

    renderWithProviders(<MovementForm />);
    await fill(user, "10000"); // R$ 100,00
    await user.click(screen.getByRole("button", { name: "Depositar" }));

    // ConfirmDialog
    const dialog = await screen.findByRole("dialog");
    await user.click(
      screen
        .getAllByRole("button", { name: "Depositar" })
        .find((b) => dialog.contains(b))!,
    );

    await waitFor(() => expect(posted).toBeDefined());
    expect(posted).toMatchObject({
      accountId: 1,
      type: "C",
      amount: 100,
      description: "teste",
    });
    expect(posted).toHaveProperty("idempotencyKey");
  });

  it("withdraw over balance: shows the backend 422 message inline", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/transactions", () =>
        HttpResponse.json(
          {
            status: 422,
            code: "INSUFFICIENT_BALANCE",
            message:
              "Saldo insuficiente para conta 1. Saldo atual: 20, valor solicitado: 50",
          },
          { status: 422 },
        ),
      ),
    );

    renderWithProviders(<MovementForm />);
    await user.click(screen.getByRole("tab", { name: "Saque" }));
    await fill(user, "5000"); // R$ 50,00
    await user.click(screen.getByRole("button", { name: "Sacar" }));

    const dialog = await screen.findByRole("dialog");
    await user.click(
      screen
        .getAllByRole("button", { name: "Sacar" })
        .find((b) => dialog.contains(b))!,
    );

    expect(
      await screen.findByText(/Saldo insuficiente para conta 1/),
    ).toBeInTheDocument();
  });

  it("picking an account in the select writes it to the ui store", async () => {
    const user = userEvent.setup();
    useUiStore.setState({ selectedAccountId: null });
    server.use(
      http.get("/api/accounts", () => HttpResponse.json([acc(1), acc(2)])),
    );

    renderWithProviders(<MovementForm />);

    const select = await screen.findByLabelText("Conta");
    await user.selectOptions(
      select,
      await screen.findByRole("option", { name: /2-2/ }),
    );

    await waitFor(() =>
      expect(useUiStore.getState().selectedAccountId).toBe(2),
    );
  });

  it("withdraw success notifies 'Saque realizado'", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/transactions", () =>
        HttpResponse.json(
          {
            transactionId: "t2",
            accountId: 1,
            type: "DEBIT",
            amount: 30,
            status: "COMPLETED",
            description: "teste",
            createdAt: "2026-09-08T10:00:00",
            transferId: null,
          },
          { status: 201 },
        ),
      ),
    );

    renderWithProviders(<MovementForm />);
    await user.click(screen.getByRole("tab", { name: "Saque" }));
    await fill(user, "3000");
    await user.click(screen.getByRole("button", { name: "Sacar" }));

    const dialog = await screen.findByRole("dialog");
    await user.click(
      screen
        .getAllByRole("button", { name: "Sacar" })
        .find((b) => dialog.contains(b))!,
    );

    await waitFor(() =>
      expect(notify.success).toHaveBeenCalledWith("Saque realizado"),
    );
  });

  it("blocks submit when the amount is below the minimum", async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.post("/api/transactions", () => {
        called = true;
        return HttpResponse.json({}, { status: 201 });
      }),
    );

    renderWithProviders(<MovementForm />);
    await fill(user, "0");
    await user.click(screen.getByRole("button", { name: "Depositar" }));

    expect(await screen.findByText("Valor mínimo R$ 0,01")).toBeInTheDocument();
    expect(called).toBe(false);
  });
});
