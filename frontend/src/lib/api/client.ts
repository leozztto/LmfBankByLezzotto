import {
  ApiError,
  apiErrorFromBody,
  type ApiErrorBody,
} from "@/lib/api/errors";

/** Anything with a `.parse` — a Zod schema, in practice. Decouples us from
 *  Zod's input-vs-output generic gymnastics on schemas that transform. */
interface Parseable<T> {
  parse: (data: unknown) => T;
}

/**
 * Browser-side HTTP client. Every call goes to `/api/...` on the same origin —
 * the Next BFF (route handlers) attaches the `Authorization: Bearer` header from
 * the httpOnly cookie and forwards to the backend (ADR 0007). So this module
 * never sees the token.
 */
export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  headers?: Record<string, string>;
  signal?: AbortSignal;
}

async function readBody(res: Response): Promise<unknown> {
  const contentType = res.headers.get("content-type") ?? "";
  const text = await res.text();
  if (!text) return undefined;
  if (contentType.includes("application/json")) {
    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }
  return text;
}

export async function apiFetch<T>(
  path: string,
  options: RequestOptions = {},
  schema?: Parseable<T>,
): Promise<T> {
  const { method = "GET", body, headers = {}, signal } = options;

  const res = await fetch(`/api/${path.replace(/^\/+/, "")}`, {
    method,
    signal,
    headers: {
      Accept: "application/json",
      ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
    cache: "no-store",
  });

  const data = await readBody(res);

  if (!res.ok) {
    if (data && typeof data === "object") {
      throw apiErrorFromBody(res.status, data as ApiErrorBody);
    }
    throw new ApiError(
      res.status,
      "UPSTREAM",
      typeof data === "string" && data ? data : res.statusText,
    );
  }

  if (schema) {
    return schema.parse(data);
  }
  return data as T;
}
