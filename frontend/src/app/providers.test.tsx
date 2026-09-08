import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useQueryClient } from "@tanstack/react-query";

import { Providers } from "./providers";

function Probe() {
  return <span>{useQueryClient() ? "has client" : "no client"}</span>;
}

describe("Providers", () => {
  it("renders children under a QueryClientProvider", () => {
    render(
      <Providers>
        <Probe />
      </Providers>,
    );

    expect(screen.getByText("has client")).toBeInTheDocument();
  });
});
