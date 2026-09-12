import { describe, expect, it } from "vitest";

import { formatDate, formatDateTime } from "./format";

describe("formatDateTime", () => {
  it("formats an ISO LocalDateTime as pt-BR", () => {
    expect(formatDateTime("2026-09-08T04:02:26.597")).toMatch(
      /^08\/09\/2026.*04:02$/,
    );
  });
  it("returns a dash for missing / invalid input", () => {
    expect(formatDateTime(null)).toBe("—");
    expect(formatDateTime("not-a-date")).toBe("—");
  });
});

describe("formatDate", () => {
  it("formats yyyy-MM-dd as dd/MM/yyyy", () => {
    expect(formatDate("2026-01-31")).toBe("31/01/2026");
  });
  it("returns a dash for missing or unparseable input", () => {
    expect(formatDate(undefined)).toBe("—");
    expect(formatDate(null)).toBe("—");
    expect(formatDate("not-a-date")).toBe("—");
  });
});
