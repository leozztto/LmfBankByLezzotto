import { beforeEach, describe, expect, it } from "vitest";

import {
  selectIsAuthenticated,
  selectUsername,
  useAuthStore,
} from "./auth-store";

describe("authStore", () => {
  beforeEach(() => useAuthStore.setState({ session: null }));

  it("sets and clears the session", () => {
    useAuthStore.getState().setSession({ authenticated: true, username: "u" });
    expect(useAuthStore.getState().session?.username).toBe("u");
    useAuthStore.getState().clearSession();
    expect(useAuthStore.getState().session).toBeNull();
  });

  it("selectors read the session", () => {
    useAuthStore.setState({
      session: { authenticated: true, username: "carol" },
    });
    const s = useAuthStore.getState();
    expect(selectIsAuthenticated(s)).toBe(true);
    expect(selectUsername(s)).toBe("carol");
    expect(selectIsAuthenticated({ ...s, session: null })).toBe(false);
  });
});
