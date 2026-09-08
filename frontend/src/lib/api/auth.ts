import { apiFetch } from "@/lib/api/client";
import { sessionSchema, type LoginInput, type Session } from "@/lib/schemas/auth";

/** These hit the Next BFF route handlers, not the backend directly. */

export function login(input: LoginInput): Promise<Session> {
  return apiFetch("auth/login", { method: "POST", body: input }, sessionSchema);
}

export function logout(): Promise<Session> {
  return apiFetch("auth/logout", { method: "POST" }, sessionSchema);
}

export function getSession(): Promise<Session> {
  return apiFetch("auth/session", { method: "GET" }, sessionSchema);
}

export type { Session };
