import { env } from "@/lib/env";
import { SESSION_COOKIE } from "@/lib/auth/constants";

export { SESSION_COOKIE };

interface CookieOptions {
  httpOnly: boolean;
  sameSite: "lax";
  secure: boolean;
  path: string;
  maxAge?: number;
}

export function sessionCookieOptions(maxAgeSeconds?: number): CookieOptions {
  return {
    httpOnly: true,
    sameSite: "lax",
    secure: env.cookieSecure,
    path: "/",
    ...(maxAgeSeconds !== undefined ? { maxAge: maxAgeSeconds } : {}),
  };
}

/**
 * Cookie lifetime in seconds. Prefers the login response's `expiresIn`; falls
 * back to the JWT's own `exp` claim; defaults to 1h if neither is available.
 */
export function resolveMaxAge(
  expiresIn: number | undefined,
  token: string,
  jwtExpiresAt: (t: string) => number | null,
): number {
  if (typeof expiresIn === "number" && expiresIn > 0) {
    return Math.floor(expiresIn);
  }
  const at = jwtExpiresAt(token);
  if (at !== null) {
    const seconds = Math.floor((at - Date.now()) / 1000);
    if (seconds > 0) return seconds;
  }
  return 3600;
}
