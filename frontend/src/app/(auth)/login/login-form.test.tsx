import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { LoginForm } from "./login-form";

const replace = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  useSearchParams: () => new URLSearchParams(""),
}));

describe("LoginForm", () => {
  beforeEach(() => replace.mockClear());

  it("shows field errors and does not call the API on an empty submit", async () => {
    let called = false;
    server.use(
      http.post("/api/auth/login", () => {
        called = true;
        return HttpResponse.json({ authenticated: true });
      }),
    );

    renderWithProviders(<LoginForm />);
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Informe o usuário")).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it("logs in and navigates to /dashboard", async () => {
    server.use(
      http.post("/api/auth/login", () =>
        HttpResponse.json({ authenticated: true, username: "demo" }),
      ),
    );

    renderWithProviders(<LoginForm />);
    await userEvent.type(screen.getByLabelText("Usuário"), "demo");
    await userEvent.type(screen.getByLabelText("Senha"), "whatever");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/dashboard"));
  });

  it("shows an error on a 401", async () => {
    server.use(
      http.post("/api/auth/login", () =>
        HttpResponse.json(
          { status: 401, code: "UNAUTHORIZED", message: "no" },
          { status: 401 },
        ),
      ),
    );

    renderWithProviders(<LoginForm />);
    await userEvent.type(screen.getByLabelText("Usuário"), "demo");
    await userEvent.type(screen.getByLabelText("Senha"), "bad");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(
      await screen.findByText("Usuário ou senha inválidos"),
    ).toBeInTheDocument();
    expect(replace).not.toHaveBeenCalled();
  });
});
