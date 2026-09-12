import { describe, expect, it } from "vitest";

import { ApiError } from "@/lib/api/errors";
import { makeQueryClient } from "./client";

describe("makeQueryClient", () => {
  it("configures sane defaults", () => {
    const qc = makeQueryClient();
    const q = qc.getDefaultOptions().queries!;
    expect(q.staleTime).toBe(30_000);
    expect(q.refetchOnWindowFocus).toBe(false);
    expect(qc.getDefaultOptions().mutations!.retry).toBe(0);
  });

  it("does not retry ApiError but retries other errors twice", () => {
    const retry = makeQueryClient().getDefaultOptions().queries!.retry as (
      n: number,
      e: Error,
    ) => boolean;
    expect(retry(0, new ApiError(500, "X", "x"))).toBe(false);
    expect(retry(0, new Error("net"))).toBe(true);
    expect(retry(2, new Error("net"))).toBe(false);
  });
});
