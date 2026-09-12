import { cookies } from "next/headers";
import { NextResponse } from "next/server";

import { SESSION_COOKIE, sessionCookieOptions } from "@/lib/auth/cookie";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

export async function POST() {
  cookies().set(SESSION_COOKIE, "", sessionCookieOptions(0));
  return NextResponse.json({ authenticated: false });
}
