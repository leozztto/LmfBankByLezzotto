import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";

import { server } from "@/test/msw/server";
import { getStatement } from "./statement";

const statementJson = {
  accountId: 1,
  balance: 150.5, // backend sends a JSON number
  startDate: null,
  endDate: null,
  transactions: [
    {
      transactionId: "11111111-1111-1111-1111-111111111111",
      accountId: 1,
      type: "CREDIT",
      amount: 150.5,
      status: "COMPLETED",
      description: "Deposit",
      createdAt: "2026-09-08T10:00:00",
      transferId: null,
    },
  ],
};

describe("getStatement", () => {
  it("hits /accounts/statement with accountId and normalises money to string", async () => {
    let url = "";
    server.use(
      http.get("/api/accounts/statement", ({ request }) => {
        url = request.url;
        return HttpResponse.json(statementJson);
      }),
    );

    const res = await getStatement(1);
    expect(new URL(url).searchParams.get("accountId")).toBe("1");
    expect(new URL(url).searchParams.has("startDate")).toBe(false);
    expect(res.balance).toBe("150.5");
    expect(res.transactions[0]!.amount).toBe("150.5");
  });

  it("adds startDate/endDate when a range is given", async () => {
    let url = "";
    server.use(
      http.get("/api/accounts/statement", ({ request }) => {
        url = request.url;
        return HttpResponse.json({
          ...statementJson,
          startDate: "2026-01-01",
          endDate: "2026-02-01",
        });
      }),
    );

    await getStatement(9, { start: "2026-01-01", end: "2026-02-01" });
    const p = new URL(url).searchParams;
    expect(p.get("accountId")).toBe("9");
    expect(p.get("startDate")).toBe("2026-01-01");
    expect(p.get("endDate")).toBe("2026-02-01");
  });
});
