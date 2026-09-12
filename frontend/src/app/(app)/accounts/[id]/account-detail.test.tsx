import { delay, http, HttpResponse } from "msw";
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

  it("renders the address row and recent transactions when present", async () => {
    server.use(
      http.get("/api/accounts/5", () =>
        HttpResponse.json({
          ...account,
          addresses: [
            {
              id: 1,
              street: "Rua A",
              number: "10",
              city: "Campinas",
              state: "SP",
              zipCode: "13000-000",
              neighborhood: "Centro",
              complement: "",
              country: "BR",
              addressType: "R",
            },
          ],
        }),
      ),
      http.get("/api/accounts/statement", () =>
        HttpResponse.json({
          accountId: 5,
          balance: 500,
          startDate: null,
          endDate: null,
          transactions: [
            {
              transactionId: "t1",
              accountId: 5,
              type: "CREDIT",
              amount: 200,
              status: "COMPLETED",
              description: "salário",
              createdAt: "2026-09-08T10:00:00",
              transferId: null,
            },
          ],
        }),
      ),
    );

    renderWithProviders(<AccountDetail id={5} />);

    expect(
      await screen.findByText("Rua A, 10 — Campinas/SP"),
    ).toBeInTheDocument();
    expect(screen.getByText("Últimos lançamentos")).toBeInTheDocument();
    expect(screen.getByText(/salário/)).toBeInTheDocument();
  });

  it("shows a skeleton for the balance while the statement loads, then the account balance as fallback", async () => {
    server.use(
      http.get("/api/accounts/5", () => HttpResponse.json(account)),
      http.get("/api/accounts/statement", async () => {
        await delay(50);
        return HttpResponse.json(
          { status: 500, code: "INTERNAL", message: "sem extrato" },
          { status: 500 },
        );
      }),
    );

    const { container } = renderWithProviders(<AccountDetail id={5} />);

    await screen.findByText("Maria Silva");
    expect(container.querySelector(".animate-pulse")).not.toBeNull();

    // statement failed -> falls back to the account's own availableBalance (10)
    expect(await screen.findByText("R$ 10,00")).toBeInTheDocument();
  });

  it("badges a blocked account as destructive and tolerates null address parts", async () => {
    server.use(
      http.get("/api/accounts/5", () =>
        HttpResponse.json({
          ...account,
          accountStatus: "B",
          addresses: [
            {
              id: 9,
              street: null,
              number: null,
              city: null,
              state: null,
              zipCode: null,
              neighborhood: null,
              complement: null,
              country: null,
              addressType: null,
            },
          ],
        }),
      ),
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

    expect(await screen.findByText("Bloqueada")).toBeInTheDocument();
    expect(screen.getByText("Endereço")).toBeInTheDocument();
    expect(screen.getByText(/—\s*\//)).toBeInTheDocument();
  });

  it("shows an error alert on a non-404 failure", async () => {
    server.use(
      http.get("/api/accounts/5", () =>
        HttpResponse.json(
          { status: 500, code: "INTERNAL", message: "explodiu" },
          { status: 500 },
        ),
      ),
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

    expect(await screen.findByText("explodiu")).toBeInTheDocument();
    expect(notFound).not.toHaveBeenCalled();
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
