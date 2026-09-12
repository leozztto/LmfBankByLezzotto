import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { CreateUserForm } from "./create-user-form";

vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

describe("CreateUserForm", () => {
  it("creates the user and clears the form", async () => {
    const user = userEvent.setup();
    let posted: Record<string, unknown> | undefined;
    server.use(
      http.post("/api/admin/users", async ({ request }) => {
        posted = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          { username: posted.username, role: "USER", accountId: null },
          { status: 201 },
        );
      }),
    );

    renderWithProviders(<CreateUserForm />);

    await user.type(screen.getByLabelText("Usuário"), "joao");
    await user.type(screen.getByLabelText("Senha"), "joao123");
    await user.click(screen.getByRole("button", { name: "Criar usuário" }));

    expect(await screen.findByLabelText("Usuário")).toHaveValue("");
    expect(posted).toMatchObject({ username: "joao", password: "joao123" });
  });

  it("shows the backend 409 (username already exists) inline", async () => {
    const user = userEvent.setup();
    server.use(
      http.post("/api/admin/users", () =>
        HttpResponse.json(
          {
            status: 409,
            code: "USERNAME_ALREADY_EXISTS",
            message: "Username already exists: joao",
          },
          { status: 409 },
        ),
      ),
    );

    renderWithProviders(<CreateUserForm />);

    await user.type(screen.getByLabelText("Usuário"), "joao");
    await user.type(screen.getByLabelText("Senha"), "joao123");
    await user.click(screen.getByRole("button", { name: "Criar usuário" }));

    expect(
      await screen.findByText("Username already exists: joao"),
    ).toBeInTheDocument();
  });

  it("blocks an empty submit", async () => {
    const user = userEvent.setup();
    let called = false;
    server.use(
      http.post("/api/admin/users", () => {
        called = true;
        return HttpResponse.json(
          { username: "x", role: "USER" },
          { status: 201 },
        );
      }),
    );

    renderWithProviders(<CreateUserForm />);
    await user.click(screen.getByRole("button", { name: "Criar usuário" }));

    expect(await screen.findByText("Informe o usuário")).toBeInTheDocument();
    expect(screen.getByText("Informe a senha")).toBeInTheDocument();
    expect(called).toBe(false);
  });
});
