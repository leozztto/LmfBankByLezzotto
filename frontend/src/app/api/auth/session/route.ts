import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { SESSION_COOKIE, sessionCookieOptions } from "@/lib/auth/cookie";
import { decodeJwt, isExpired } from "@/lib/auth/jwt";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function GET() {
  const token = cookies().get(SESSION_COOKIE)?.value;

  if (!token || isExpired(token)) {
    if (token) cookies().set(SESSION_COOKIE, "", sessionCookieOptions(0));
    return NextResponse.json({ authenticated: false });
  }

  const claims = decodeJwt(token);
  return NextResponse.json({
    authenticated: true,
    username: claims?.sub,
    role: claims?.role,
    expiresAt: typeof claims?.exp === "number" ? claims.exp * 1000 : undefined,
  });
}
