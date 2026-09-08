import { http, HttpResponse } from "msw";

/**
 * Happy-path handlers. They match BOTH the backend origin (hit by the BFF route
 * handlers in node tests) and same-origin `/api/*` (hit by the browser client in
 * jsdom tests). Individual tests override with `server.use(...)` for error paths.
 */

const BACKEND = "http://backend:8080";
const LOCAL = "http://localhost:8080";

// A syntactically valid unsigned JWT with sub=demo, exp far in the future.
const header = Buffer.from(JSON.stringify({ alg: "HS256", typ: "JWT" })).toString("base64url");
const payload = Buffer.from(
  JSON.stringify({ sub: "demo", exp: Math.floor(Date.now() / 1000) + 86400 }),
).toString("base64url");
export const FAKE_JWT = `${header}.${payload}.signature`;

function loginHandler() {
  return HttpResponse.json({
    token: FAKE_JWT,
    tokenType: "Bearer",
    expiresIn: 86400,
  });
}

function healthHandler() {
  return HttpResponse.json({ status: "UP" });
}

export const handlers = [
  http.post(`${BACKEND}/auth/login`, loginHandler),
  http.post(`${LOCAL}/auth/login`, loginHandler),
  http.post("/api/auth/login", loginHandler),

  http.get(`${BACKEND}/actuator/health`, healthHandler),
  http.get(`${LOCAL}/actuator/health`, healthHandler),
  http.get("/api/actuator/health", healthHandler),
];
