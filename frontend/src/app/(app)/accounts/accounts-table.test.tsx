import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { AccountsTable } from "./accounts-table";

const acc = (id: number, over: Record<string, unknown> = {}) => ({
  accountId: id,
  fullName: "Maria Silva",
  maskedDocument: "***.982.***-25",
  maskedEmail: "m***@e.com",
  maskedPhone: "(11) *****-1",
  accountType: "C",
  accountNumber: `1234567${id}-9`,
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: 0, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
  ...over,
});

describe("AccountsTable", () => {
  it("renders a row per account with the masked document", async () => {
    server.use(
      http.get("/api/accounts", () =>
        HttpResponse.json([acc(1), acc(2, { accountStatus: "B" })]),
      ),
    );

    renderWithProviders(<AccountsTable />);

    expect(await screen.findByText("12345671-9")).toBeInTheDocument();
    expect(screen.getByText("12345672-9")).toBeInTheDocument();
    expect(screen.getAllByText("***.982.***-25")).toHaveLength(2);
    expect(screen.getByText("Bloqueada")).toBeInTheDocument();
  });

  it("shows an empty state when there are no accounts", async () => {
    server.use(http.get("/api/accounts", () => HttpResponse.json([])));
    renderWithProviders(<AccountsTable />);
    expect(await screen.findByText("Nenhuma conta")).toBeInTheDocument();
  });

  it("shows an error alert when the request fails", async () => {
    server.use(
      http.get("/api/accounts", () =>
        HttpResponse.json(
          { status: 500, code: "INTERNAL", message: "falha ao listar" },
          { status: 500 },
        ),
      ),
    );
    renderWithProviders(<AccountsTable />);
    expect(await screen.findByText("falha ao listar")).toBeInTheDocument();
  });
});
