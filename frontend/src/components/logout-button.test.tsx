import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { useAuthStore } from "@/stores/auth-store";
import { useUiStore } from "@/stores/ui-store";
import { LogoutButton } from "./logout-button";

const replace = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ replace }) }));

describe("LogoutButton", () => {
  beforeEach(() => {
    replace.mockClear();
    useAuthStore.setState({ session: { authenticated: true, username: "u" } });
    useUiStore.setState({ selectedAccountId: 3 });
  });

  it("logs out: clears state and navigates to /login", async () => {
    server.use(
      http.post("/api/auth/logout", () =>
        HttpResponse.json({ authenticated: false }),
      ),
    );

    renderWithProviders(<LogoutButton />);
    await userEvent.click(screen.getByRole("button", { name: "Sair" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(useAuthStore.getState().session).toBeNull();
    expect(useUiStore.getState().selectedAccountId).toBeNull();
  });
});
