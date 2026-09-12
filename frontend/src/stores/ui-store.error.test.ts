import { afterEach, describe, expect, it, vi } from "vitest";

describe("uiStore storage resilience", () => {
  afterEach(() => vi.restoreAllMocks());

  it("does not throw when localStorage access fails", async () => {
    const throwing = {
      getItem: () => {
        throw new Error("blocked");
      },
      setItem: () => {
        throw new Error("blocked");
      },
      removeItem: () => {
        throw new Error("blocked");
      },
    };
    vi.stubGlobal("localStorage", throwing);
    vi.resetModules();

    const { useUiStore } = await import("./ui-store");
    expect(() => useUiStore.getState().setSelectedAccount(5)).not.toThrow();
    expect(() => useUiStore.persist.rehydrate()).not.toThrow();
    expect(() => useUiStore.getState().reset()).not.toThrow();

    vi.unstubAllGlobals();
  });
});
