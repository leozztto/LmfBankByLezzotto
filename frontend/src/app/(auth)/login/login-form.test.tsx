import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { LoginForm } from "./login-form";

const replace = vi.fn();
let search = "";
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace }),
  useSearchParams: () => new URLSearchParams(search),
}));

describe("LoginForm", () => {
  beforeEach(() => {
    replace.mockClear();
    search = "";
  });

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

  it("redirects to a safe ?next path after login", async () => {
    search = "next=/accounts";
    server.use(
      http.post("/api/auth/login", () =>
        HttpResponse.json({ authenticated: true, username: "demo" }),
      ),
    );

    renderWithProviders(<LoginForm />);
    await userEvent.type(screen.getByLabelText("Usuário"), "demo");
    await userEvent.type(screen.getByLabelText("Senha"), "whatever");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/accounts"));
  });

  it("ignores an unsafe ?next (non-relative) and goes to /dashboard", async () => {
    search = "next=https://evil.example";
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

  it("shows the backend message for a non-401 ApiError", async () => {
    server.use(
      http.post("/api/auth/login", () =>
        HttpResponse.json(
          { status: 423, code: "LOCKED", message: "Conta bloqueada" },
          { status: 423 },
        ),
      ),
    );

    renderWithProviders(<LoginForm />);
    await userEvent.type(screen.getByLabelText("Usuário"), "demo");
    await userEvent.type(screen.getByLabelText("Senha"), "x");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(await screen.findByText("Conta bloqueada")).toBeInTheDocument();
  });

  it("shows a generic message when the request fails without an ApiError", async () => {
    server.use(http.post("/api/auth/login", () => HttpResponse.error()));

    renderWithProviders(<LoginForm />);
    await userEvent.type(screen.getByLabelText("Usuário"), "demo");
    await userEvent.type(screen.getByLabelText("Senha"), "x");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));

    expect(
      await screen.findByText("Não foi possível entrar. Tente novamente."),
    ).toBeInTheDocument();
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
