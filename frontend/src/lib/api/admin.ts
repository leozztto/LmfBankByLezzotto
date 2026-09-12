import { apiFetch } from "@/lib/api/client";
import {
  appUserResponseSchema,
  type AppUserResponse,
  type CreateUserFormValues,
} from "@/lib/schemas/admin";

/** Admin-only (ADR 0010) — the backend rejects these with 403 for anyone else. */

export function createUser(
  payload: CreateUserFormValues,
): Promise<AppUserResponse> {
  return apiFetch(
    "admin/users",
    { method: "POST", body: payload },
    appUserResponseSchema,
  );
}

export function linkAccount(
  username: string,
  accountId: number,
): Promise<void> {
  return apiFetch(`admin/users/${encodeURIComponent(username)}/account`, {
    method: "PATCH",
    body: { accountId },
  });
}
