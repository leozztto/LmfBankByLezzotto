import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { z } from "zod";

import { server } from "@/test/msw/server";
import { apiFetch } from "./client";
import { ApiError, ValidationError } from "./errors";

describe("apiFetch", () => {
  it("returns parsed data on success when a schema is given", async () => {
    server.use(
      http.get("/api/ping", () => HttpResponse.json({ ok: true, n: 1 })),
    );
    const data = await apiFetch(
      "ping",
      {},
      z.object({ ok: z.boolean(), n: z.number() }),
    );
    expect(data).toEqual({ ok: true, n: 1 });
  });

  it("sends a JSON body on POST", async () => {
    let received: unknown;
    server.use(
      http.post("/api/echo", async ({ request }) => {
        received = await request.json();
        return HttpResponse.json({ ok: true });
      }),
    );
    await apiFetch("echo", { method: "POST", body: { a: 1 } });
    expect(received).toEqual({ a: 1 });
  });

  it("maps a 404 ApiError body", async () => {
    server.use(
      http.get("/api/missing", () =>
        HttpResponse.json(
          { status: 404, code: "ACCOUNT_NOT_FOUND", message: "não achou" },
          { status: 404 },
        ),
      ),
    );
    await expect(apiFetch("missing")).rejects.toMatchObject({
      status: 404,
      code: "ACCOUNT_NOT_FOUND",
    });
  });

  it("maps a 400 fieldErrors body to ValidationError", async () => {
    server.use(
      http.post("/api/accounts", () =>
        HttpResponse.json(
          {
            status: 400,
            code: "VALIDATION_ERROR",
            message: "Falha de validação",
            fieldErrors: { email: "must be a well-formed email address" },
          },
          { status: 400 },
        ),
      ),
    );
    const err = (await apiFetch("accounts", { method: "POST", body: {} }).catch(
      (e) => e,
    )) as ApiError;
    expect(err).toBeInstanceOf(ValidationError);
    expect(err.fieldErrors).toEqual({
      email: "must be a well-formed email address",
    });
  });

  it("maps a 422 INSUFFICIENT_BALANCE", async () => {
    server.use(
      http.post("/api/transactions", () =>
        HttpResponse.json(
          {
            status: 422,
            code: "INSUFFICIENT_BALANCE",
            message: "Saldo insuficiente",
          },
          { status: 422 },
        ),
      ),
    );
    await expect(
      apiFetch("transactions", { method: "POST", body: {} }),
    ).rejects.toMatchObject({ status: 422, code: "INSUFFICIENT_BALANCE" });
  });

  it("maps a 401 to ApiError(401)", async () => {
    server.use(
      http.get("/api/secure", () =>
        HttpResponse.json(
          { status: 401, code: "UNAUTHORIZED", message: "no" },
          { status: 401 },
        ),
      ),
    );
    const err = (await apiFetch("secure").catch((e) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.status).toBe(401);
  });

  it("wraps a non-JSON error body as ApiError('UPSTREAM')", async () => {
    server.use(
      http.get(
        "/api/boom",
        () => new HttpResponse("502 Bad Gateway", { status: 502 }),
      ),
    );
    const err = (await apiFetch("boom").catch((e) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.code).toBe("UPSTREAM");
  });

  it("falls back to statusText when an error body is empty", async () => {
    server.use(
      http.get("/api/empty", () => new HttpResponse(null, { status: 503 })),
    );
    const err = (await apiFetch("empty").catch((e) => e)) as ApiError;
    expect(err).toBeInstanceOf(ApiError);
    expect(err.code).toBe("UPSTREAM");
    expect(err.status).toBe(503);
  });

  it("returns the raw text when a JSON content-type carries invalid JSON", async () => {
    server.use(
      http.get(
        "/api/badjson",
        () =>
          new HttpResponse("{not json", {
            status: 200,
            headers: { "content-type": "application/json" },
          }),
      ),
    );
    await expect(apiFetch("badjson")).resolves.toBe("{not json");
  });
});
