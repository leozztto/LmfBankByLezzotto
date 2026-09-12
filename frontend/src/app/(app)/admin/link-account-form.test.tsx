import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { LinkAccountForm } from "./link-account-form";

vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

describe("LinkAccountForm", () => {
  it("links the account and clears the form", async () => {
    const user = userEvent.setup();
    let patched: Record<string, unknown> | undefined;
    server.use(
      http.patch("/api/admin/users/:username/account", async ({ request }) => {
        patched = (await request.json()) as Record<string, unknown>;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<LinkAccountForm />);

    await user.type(screen.getByLabelText("Usuário a vincular"), "demo");
    await user.type(screen.getByLabelText("Id da conta"), "7");
    await user.click(screen.getByRole("button", { name: "Vincular conta" }));

    expect(await screen.findByLabelText("Usuário a vincular")).toHaveValue("");
    expect(patched).toMatchObject({ accountId: 7 });
  });

  it("shows the backend 409 (account already linked) inline", async () => {
    const user = userEvent.setup();
    server.use(
      http.patch("/api/admin/users/:username/account", () =>
        HttpResponse.json(
          {
            status: 409,
            code: "ACCOUNT_ALREADY_LINKED",
            message: "Account 7 is already linked to another user",
          },
          { status: 409 },
        ),
      ),
    );

    renderWithProviders(<LinkAccountForm />);

    await user.type(screen.getByLabelText("Usuário a vincular"), "demo");
    await user.type(screen.getByLabelText("Id da conta"), "7");
    await user.click(screen.getByRole("button", { name: "Vincular conta" }));

    expect(
      await screen.findByText("Account 7 is already linked to another user"),
    ).toBeInTheDocument();
  });

  it("blocks submit without a username or a positive account id", async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.patch("/api/admin/users/:username/account", () => {
        called = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );

    renderWithProviders(<LinkAccountForm />);
    await user.click(screen.getByRole("button", { name: "Vincular conta" }));

    expect(await screen.findByText("Informe o usuário")).toBeInTheDocument();
    expect(screen.getByText("Informe o id da conta")).toBeInTheDocument();
    expect(called).toBe(false);
  });
});
