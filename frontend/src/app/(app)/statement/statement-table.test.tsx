import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useUiStore } from "@/stores/ui-store";
import { StatementTable } from "./statement-table";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

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

const txn = (over: Record<string, unknown>) => ({
  transactionId: `t${Math.random()}`,
  accountId: 1,
  type: "CREDIT",
  amount: 100,
  status: "COMPLETED",
  description: "Deposit",
  createdAt: "2026-09-08T10:00:00",
  transferId: null,
  ...over,
});

describe("StatementTable", () => {
  beforeEach(() => {
    useUiStore.setState({ selectedAccountId: 1 });
    server.use(http.get("/api/accounts", () => HttpResponse.json([acc(1)])));
  });

  it("renders entries with PT labels and signed amounts", async () => {
    server.use(
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 1,
          balance: 150,
          startDate: null,
          endDate: null,
          transactions: [
            txn({ type: "CREDIT", amount: 200, description: "salario" }),
            txn({
              type: "DEBIT",
              amount: 50,
              description: "saque",
              status: "COMPLETED",
            }),
          ],
        }),
      ),
    );

    renderWithProviders(<StatementTable />);

    expect(await screen.findByText("salario")).toBeInTheDocument();
    expect(screen.getByText("R$ 200,00")).toBeInTheDocument();
    expect(screen.getByText("-R$ 50,00")).toBeInTheDocument();
    expect(screen.getAllByText("Crédito").length).toBeGreaterThan(0);
    expect(screen.getByText("Débito")).toBeInTheDocument();
  });

  it("badges a non-completed entry as destructive", async () => {
    server.use(
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 1,
          balance: 0,
          startDate: null,
          endDate: null,
          transactions: [txn({ status: "FAILED", description: "estorno" })],
        }),
      ),
    );

    renderWithProviders(<StatementTable />);

    expect(await screen.findByText("Falhou")).toBeInTheDocument();
  });

  it("prompts to pick an account when none is selected", async () => {
    useUiStore.setState({ selectedAccountId: null });
    server.use(
      http.get("/api/accounts", () => HttpResponse.json([acc(1), acc(2)])),
    );

    renderWithProviders(<StatementTable />);

    expect(await screen.findByText("Selecione uma conta")).toBeInTheDocument();
  });

  it("choosing an account in the select updates the ui store", async () => {
    useUiStore.setState({ selectedAccountId: null });
    server.use(
      http.get("/api/accounts", () => HttpResponse.json([acc(1), acc(2)])),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 2,
          balance: 0,
          startDate: null,
          endDate: null,
          transactions: [],
        }),
      ),
    );

    renderWithProviders(<StatementTable />);

    const select = await screen.findByRole("combobox");
    await userEvent.selectOptions(
      select,
      await screen.findByRole("option", { name: /200-2/ }),
    );

    await waitFor(() =>
      expect(useUiStore.getState().selectedAccountId).toBe(2),
    );
  });

  it("shows an error alert when the statement request fails", async () => {
    server.use(
      http.get("/api/accounts/statement", () =>
        HttpResponse.json(
          { status: 500, code: "INTERNAL", message: "falhou o extrato" },
          { status: 500 },
        ),
      ),
    );

    renderWithProviders(<StatementTable />);

    expect(await screen.findByText("falhou o extrato")).toBeInTheDocument();
  });

  it("applying a date range refetches with the period", async () => {
    let lastUrl = "";
    server.use(
      http.get("/api/accounts/statement", ({ request }) => {
        lastUrl = request.url;
        return HttpResponse.json({
          accountId: 1,
          balance: 0,
          startDate: null,
          endDate: null,
          transactions: [],
        });
      }),
    );

    renderWithProviders(<StatementTable />);
    await screen.findByText("Sem lançamentos");

    await userEvent.type(screen.getByLabelText("De"), "2026-01-01");
    await userEvent.type(screen.getByLabelText("Até"), "2026-02-01");
    await userEvent.click(screen.getByRole("button", { name: "Aplicar" }));

    await waitFor(() =>
      expect(new URL(lastUrl).searchParams.get("startDate")).toBe("2026-01-01"),
    );
  });
});
