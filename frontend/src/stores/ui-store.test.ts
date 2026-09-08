import { beforeEach, describe, expect, it } from "vitest";

import { useUiStore } from "./ui-store";

describe("uiStore", () => {
  beforeEach(() => {
    window.localStorage.clear();
    useUiStore.setState({ selectedAccountId: null });
  });

  it("stores and resets the selected account", () => {
    useUiStore.getState().setSelectedAccount(42);
    expect(useUiStore.getState().selectedAccountId).toBe(42);
    useUiStore.getState().reset();
    expect(useUiStore.getState().selectedAccountId).toBeNull();
  });

  it("persists to localStorage under lmf-ui", () => {
    useUiStore.getState().setSelectedAccount(7);
    const raw = window.localStorage.getItem("lmf-ui");
    expect(raw).toContain('"selectedAccountId":7');
  });

  it("rehydrates from an existing localStorage value", async () => {
    window.localStorage.setItem(
      "lmf-ui",
      JSON.stringify({ state: { selectedAccountId: 99 }, version: 0 }),
    );
    useUiStore.persist.rehydrate();
    expect(useUiStore.getState().selectedAccountId).toBe(99);
  });
});
