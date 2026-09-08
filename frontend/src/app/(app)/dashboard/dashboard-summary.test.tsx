import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { DashboardSummary } from "./dashboard-summary";

const acc = (id: number) => ({
  accountId: id,
  fullName: "X",
  maskedDocument: "***",
  maskedEmail: "x***@e.com",
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

describe("DashboardSummary", () => {
  it("shows the account count and quick-action links", async () => {
    server.use(
      http.get("/api/accounts", () =>
        HttpResponse.json([acc(1), acc(2), acc(3)]),
      ),
    );

    renderWithProviders(<DashboardSummary />);

    expect(await screen.findByText("3")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver todas" })).toHaveAttribute(
      "href",
      "/accounts",
    );
    expect(
      screen.getByRole("link", { name: "Abrir uma conta" }),
    ).toBeInTheDocument();
  });

  it("shows 0 when there are no accounts", async () => {
    server.use(http.get("/api/accounts", () => HttpResponse.json([])));
    renderWithProviders(<DashboardSummary />);
    expect(await screen.findByText("0")).toBeInTheDocument();
  });
});
