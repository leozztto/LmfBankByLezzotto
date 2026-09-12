import "server-only";

/**
 * Server-only runtime config. `BACKEND_ORIGIN` is where the BFF route handlers
 * forward `/api/*` — `http://localhost:8080` under `next dev`, `http://backend:8080`
 * in the compose stack (injected by docker-compose).
 */
export const env = {
  BACKEND_ORIGIN: process.env.BACKEND_ORIGIN ?? "http://localhost:8080",
  /** Only send `Secure` on the session cookie behind TLS (see ADR 0007). */
  cookieSecure: process.env.COOKIE_SECURE === "true",
};
