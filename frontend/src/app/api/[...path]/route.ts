import { cookies } from "next/headers";
import { NextResponse, type NextRequest } from "next/server";

import { env } from "@/lib/env";
import { SESSION_COOKIE } from "@/lib/auth/cookie";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

/**
 * BFF catch-all: forwards every `/api/<path>` call to `${BACKEND_ORIGIN}/<path>`,
 * injecting `Authorization: Bearer <jwt>` from the httpOnly session cookie. The
 * browser never sends or sees the token (ADR 0007).
 */
async function proxy(request: NextRequest, path: string[]) {
  const target = `${env.BACKEND_ORIGIN}/${path.join("/")}${request.nextUrl.search}`;

  const headers = new Headers();
  const contentType = request.headers.get("content-type");
  const accept = request.headers.get("accept");
  if (contentType) headers.set("content-type", contentType);
  if (accept) headers.set("accept", accept);

  const jwt = cookies().get(SESSION_COOKIE)?.value;
  if (jwt) headers.set("authorization", `Bearer ${jwt}`);

  const hasBody = !["GET", "HEAD"].includes(request.method);
  const body = hasBody ? await request.arrayBuffer() : undefined;

  const upstream = await fetch(target, {
    method: request.method,
    headers,
    body: body && body.byteLength > 0 ? body : undefined,
    redirect: "manual",
    cache: "no-store",
  });

  return new NextResponse(upstream.body, {
    status: upstream.status,
    headers: {
      "content-type":
        upstream.headers.get("content-type") ?? "application/json",
    },
  });
}

type Ctx = { params: { path: string[] } };

export const GET = (req: NextRequest, { params }: Ctx) =>
  proxy(req, params.path);
export const POST = (req: NextRequest, { params }: Ctx) =>
  proxy(req, params.path);
export const PUT = (req: NextRequest, { params }: Ctx) =>
  proxy(req, params.path);
export const PATCH = (req: NextRequest, { params }: Ctx) =>
  proxy(req, params.path);
export const DELETE = (req: NextRequest, { params }: Ctx) =>
  proxy(req, params.path);
