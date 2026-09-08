import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { DocumentSearch } from "./document-search";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));

describe("DocumentSearch", () => {
  beforeEach(() => push.mockClear());

  it("shows an error for an invalid CPF and does not search", async () => {
    let called = false;
    server.use(
      http.get("/api/accounts/document/:doc", () => {
        called = true;
        return HttpResponse.json({}, { status: 200 });
      }),
    );

    renderWithProviders(<DocumentSearch />);
    await userEvent.type(screen.getByLabelText("CPF"), "11111111111");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(await screen.findByText("CPF inválido")).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it("navigates to the account when the CPF is found", async () => {
    server.use(
      http.get("/api/accounts/document/52998224725", () =>
        HttpResponse.json({
          accountId: 7,
          fullName: "Maria",
          maskedDocument: "***",
          maskedEmail: "m***@e.com",
          maskedPhone: "(11) *****-1",
          accountType: "C",
          accountNumber: "1-2",
          agency: "0001",
          accountStatus: "A",
          createdAt: "2026-09-08T10:00:00",
          updatedAt: null,
          balance: { availableBalance: 0, blockedBalance: 0, totalBalance: 0 },
          addresses: [],
        }),
      ),
    );

    renderWithProviders(<DocumentSearch />);
    await userEvent.type(screen.getByLabelText("CPF"), "52998224725");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/accounts/7"));
  });

  it("shows 'not found' when the CPF has no account", async () => {
    server.use(
      http.get("/api/accounts/document/52998224725", () =>
        HttpResponse.json(
          { status: 404, code: "ACCOUNT_NOT_FOUND", message: "x" },
          { status: 404 },
        ),
      ),
    );

    renderWithProviders(<DocumentSearch />);
    await userEvent.type(screen.getByLabelText("CPF"), "52998224725");
    await userEvent.click(screen.getByRole("button", { name: "Buscar" }));

    expect(
      await screen.findByText("Nenhuma conta para esse CPF"),
    ).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
