import { http, HttpResponse } from "msw";
import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { AppShell } from "./app-shell";

vi.mock("next/navigation", () => ({
  usePathname: () => "/dashboard",
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
}));

describe("AppShell", () => {
  it("renders the chrome (sidebar, switcher, logout, username) around the children", async () => {
    server.use(http.get("/api/accounts", () => HttpResponse.json([])));

    renderWithProviders(
      <AppShell session={{ authenticated: true, username: "carol" }}>
        <p>page body</p>
      </AppShell>,
    );

    expect(screen.getByText("page body")).toBeInTheDocument();
    expect(screen.getByText("carol")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Painel" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Sair" })).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Pular para o conteúdo" }),
    ).toBeInTheDocument();
  });

  it("falls back to 'sessão' when the session has no username", async () => {
    server.use(http.get("/api/accounts", () => HttpResponse.json([])));

    renderWithProviders(
      <AppShell session={{ authenticated: true }}>
        <p>body</p>
      </AppShell>,
    );

    expect(screen.getByText("sessão")).toBeInTheDocument();
  });
});
