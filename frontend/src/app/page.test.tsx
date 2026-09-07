import { render, screen } from "@testing-library/react";
import { afterEach, expect, test, vi } from "vitest";
import Home from "./page";

afterEach(() => {
  vi.unstubAllGlobals();
});

test("mostra o status de saúde do backend retornado por /api/actuator/health", async () => {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ json: () => Promise.resolve({ status: "UP" }) }),
  );

  render(<Home />);

  expect(await screen.findByText("backend health: UP")).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith("/api/actuator/health");
});

test("mostra 'unreachable' quando o fetch falha", async () => {
  vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("network")));

  render(<Home />);

  expect(await screen.findByText("backend health: unreachable")).toBeInTheDocument();
});
