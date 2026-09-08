import { render } from "@testing-library/react";
import { beforeEach, describe, expect, it } from "vitest";

import { useAuthStore } from "@/stores/auth-store";
import { SessionBootstrap } from "./session-bootstrap";

describe("SessionBootstrap", () => {
  beforeEach(() => useAuthStore.setState({ session: null }));

  it("seeds the auth store from the server session", () => {
    render(
      <SessionBootstrap session={{ authenticated: true, username: "demo" }} />,
    );
    expect(useAuthStore.getState().session).toMatchObject({ username: "demo" });
  });
});
