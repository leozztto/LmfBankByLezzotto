import { NextRequest } from "next/server";
import { vi } from "vitest";

/**
 * Utilitários para exercitar a camada BFF (route handlers do Next) contra o mock
 * server do Pact. Quem fala HTTP com o backend de verdade é o BFF — o client do
 * browser (`src/lib/api/*`) só chama `/api/*` na mesma origem (ADR 0007). Então o
 * contrato "o que o front espera da API" mora aqui.
 *
 * Cada arquivo de teste deve declarar seu próprio mock de `next/headers`:
 *
 *   vi.mock("next/headers", () => ({
 *     cookies: () => ({ get: (n: string) => (n === "lmf_token" ? { value: "x" } : undefined) }),
 *   }));
 */

type Method = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

/** Aponta o BFF para o mock server e recarrega os módulos que leem `BACKEND_ORIGIN`. */
function pointBackendAt(url: string): void {
  process.env.BACKEND_ORIGIN = url.replace(/\/$/, "");
  vi.resetModules();
}

function jsonHeaders(withBody: boolean): Record<string, string> {
  return {
    accept: "application/json",
    ...(withBody ? { "content-type": "application/json" } : {}),
  };
}

/** `POST /api/auth/login` — rota dedicada (`src/app/api/auth/login/route.ts`). */
export async function callLogin(
  backendUrl: string,
  body: unknown,
): Promise<Response> {
  pointBackendAt(backendUrl);
  const { POST } = await import("@/app/api/auth/login/route");
  return POST(
    new Request("http://localhost/api/auth/login", {
      method: "POST",
      headers: jsonHeaders(true),
      body: JSON.stringify(body),
    }),
  );
}

/**
 * Qualquer outra chamada — passa pelo catch-all `src/app/api/[...path]/route.ts`,
 * que injeta `Authorization: Bearer <jwt>` do cookie de sessão.
 */
export async function callApi(
  backendUrl: string,
  method: Method,
  apiPath: string,
  opts: { body?: unknown; search?: string } = {},
): Promise<Response> {
  pointBackendAt(backendUrl);
  const route = (await import(
    "@/app/api/[...path]/route"
  )) as unknown as Record<
    Method,
    (req: NextRequest, ctx: { params: { path: string[] } }) => Promise<Response>
  >;

  const search = opts.search ? `?${opts.search.replace(/^\?/, "")}` : "";
  const segments = apiPath.replace(/^\/+/, "").split("/");
  const hasBody = opts.body !== undefined;

  const req = new NextRequest(
    new Request(`http://localhost/api/${segments.join("/")}${search}`, {
      method,
      headers: jsonHeaders(hasBody),
      body: hasBody ? JSON.stringify(opts.body) : undefined,
    }),
  );

  return route[method](req, { params: { path: segments } });
}
