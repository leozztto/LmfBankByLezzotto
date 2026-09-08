import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useUiStore } from "@/stores/ui-store";
import { AccountDetail } from "./account-detail";

const notFound = vi.fn();
vi.mock("next/navigation", () => ({ notFound: () => notFound() }));

const account = {
  accountId: 5,
  fullName: "Maria Silva",
  maskedDocument: "***.982.***-25",
  maskedEmail: "ma***@example.com",
  maskedPhone: "(11) *****-4321",
  accountType: "C",
  accountNumber: "12345678-9",
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T04:02:26.597",
  updatedAt: null,
  balance: { availableBalance: 10, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
};

describe("AccountDetail", () => {
  beforeEach(() => {
    notFound.mockClear();
    useUiStore.setState({ selectedAccountId: null });
  });

  it("shows masked data and the balance from the statement (not the account)", async () => {
    server.use(
      http.get("/api/accounts/5", () => HttpResponse.json(account)),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 5,
          balance: 1234.56,
          startDate: null,
          endDate: null,
          transactions: [],
        }),
      ),
    );

    renderWithProviders(<AccountDetail id={5} />);

    expect(await screen.findByText("ma***@example.com")).toBeInTheDocument();
    expect(await screen.findByText("R$ 1.234,56")).toBeInTheDocument();
    // the account's own availableBalance (10) is NOT what we show
    expect(screen.queryByText("R$ 10,00")).not.toBeInTheDocument();
  });

  it("selects the account in the ui store once loaded", async () => {
    server.use(
      http.get("/api/accounts/5", () => HttpResponse.json(account)),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 5,
          balance: 0,
          startDate: null,
          endDate: null,
          transactions: [],
        }),
      ),
    );

    renderWithProviders(<AccountDetail id={5} />);
    await screen.findByText("Maria Silva");
    expect(useUiStore.getState().selectedAccountId).toBe(5);
  });

  it("calls notFound() on a 404", async () => {
    server.use(
      http.get("/api/accounts/999", () =>
        HttpResponse.json(
          { status: 404, code: "ACCOUNT_NOT_FOUND", message: "não achou" },
          { status: 404 },
        ),
      ),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 999,
          balance: 0,
          startDate: null,
          endDate: null,
          transactions: [],
        }),
      ),
    );

    renderWithProviders(<AccountDetail id={999} />);
    await vi.waitFor(() => expect(notFound).toHaveBeenCalled());
  });
});
