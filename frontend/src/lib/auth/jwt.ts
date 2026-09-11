/**
 * Decodes a JWT payload WITHOUT verifying the signature. The frontend never has
 * the signing secret — verification is always the backend's job (ADR 0007). This
 * is only used to read `sub` / `exp` for UI purposes (showing the username,
 * knowing when the session lapses).
 */
export interface JwtPayload {
  sub?: string;
  exp?: number;
  iat?: number;
  /** ADR 0010 — "ADMIN" or "USER"; absent on a token minted before that ADR. */
  role?: string;
  accountId?: number;
}

export function decodeJwt(token: string): JwtPayload | null {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const payload = parts[1];
  if (!payload) return null;
  try {
    const json = Buffer.from(
      payload.replace(/-/g, "+").replace(/_/g, "/"),
      "base64",
    ).toString("utf8");
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

/** ms epoch when the token expires, or null if it has no `exp`. */
export function jwtExpiresAt(token: string): number | null {
  const exp = decodeJwt(token)?.exp;
  return typeof exp === "number" ? exp * 1000 : null;
}

export function isExpired(token: string, now = Date.now()): boolean {
  const at = jwtExpiresAt(token);
  return at !== null && at <= now;
}
