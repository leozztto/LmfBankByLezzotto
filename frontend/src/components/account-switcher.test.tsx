import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useUiStore } from "@/stores/ui-store";
import { AccountSwitcher } from "./account-switcher";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

const acc = (id: number, number: string, name: string) => ({
  accountId: id,
  fullName: name,
  maskedDocument: "***",
  maskedEmail: "m***@e.com",
  maskedPhone: "(11) *****-1",
  accountType: "C",
  accountNumber: number,
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: 0, blockedBalance: 0, totalBalance: 0 },
  addresses: [],
});

describe("AccountSwitcher", () => {
  beforeEach(() => {
    useUiStore.setState({ selectedAccountId: null });
    server.use(
      http.get("/api/accounts", () =>
        HttpResponse.json([acc(1, "111-1", "Ana"), acc(2, "222-2", "Bruno")]),
      ),
    );
  });

  it("shows a placeholder label when nothing is selected", async () => {
    renderWithProviders(<AccountSwitcher />);
    expect(
      await screen.findByRole("button", { name: /Selecionar conta/i }),
    ).toBeInTheDocument();
  });

  it("shows the selected account label from the ui store", async () => {
    useUiStore.setState({ selectedAccountId: 2 });
    renderWithProviders(<AccountSwitcher />);
    expect(await screen.findByText("222-2 · Bruno")).toBeInTheDocument();
  });
});
