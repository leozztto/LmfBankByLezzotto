"use client";

import { useRef } from "react";

import type { Session } from "@/lib/schemas/auth";
import { useAuthStore } from "@/stores/auth-store";

/**
 * Seeds the auth store from the session the server read out of the httpOnly
 * cookie. Runs once on mount (the cookie is the source of truth; this is UX).
 */
export function SessionBootstrap({ session }: { session: Session }) {
  const done = useRef(false);
  if (!done.current) {
    done.current = true;
    useAuthStore.getState().setSession(session);
  }
  return null;
}
