import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { env } from "@/lib/env";
import {
  SESSION_COOKIE,
  resolveMaxAge,
  sessionCookieOptions,
} from "@/lib/auth/cookie";
import { decodeJwt, jwtExpiresAt } from "@/lib/auth/jwt";
import { loginResponseSchema, loginSchema } from "@/lib/schemas/auth";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST(request: Request) {
  let credentials;
  try {
    credentials = loginSchema.parse(await request.json());
  } catch {
    return NextResponse.json(
      {
        status: 400,
        code: "VALIDATION_ERROR",
        message: "Usuário e senha são obrigatórios",
      },
      { status: 400 },
    );
  }

  const upstream = await fetch(`${env.BACKEND_ORIGIN}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify(credentials),
    cache: "no-store",
  });

  const body = await upstream.text();

  if (!upstream.ok) {
    return new NextResponse(body, {
      status: upstream.status,
      headers: {
        "content-type":
          upstream.headers.get("content-type") ?? "application/json",
      },
    });
  }

  let parsed;
  try {
    parsed = loginResponseSchema.parse(JSON.parse(body));
  } catch {
    return NextResponse.json(
      {
        status: 502,
        code: "UPSTREAM",
        message: "Resposta de login inesperada do backend",
      },
      { status: 502 },
    );
  }

  const maxAge = resolveMaxAge(parsed.expiresIn, parsed.token, jwtExpiresAt);
  cookies().set(SESSION_COOKIE, parsed.token, sessionCookieOptions(maxAge));

  const claims = decodeJwt(parsed.token);
  return NextResponse.json({
    authenticated: true,
    username: claims?.sub ?? credentials.username,
    expiresAt: Date.now() + maxAge * 1000,
  });
}
