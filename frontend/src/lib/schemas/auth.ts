import { z } from "zod";

export const loginSchema = z.object({
  username: z.string().min(1, "Informe o usuário"),
  password: z.string().min(1, "Informe a senha"),
});
export type LoginInput = z.infer<typeof loginSchema>;

/** POST /auth/login response, after Fase 3.0 (JSON). */
export const loginResponseSchema = z.object({
  token: z.string().min(1),
  tokenType: z.string().default("Bearer"),
  expiresIn: z.number().nonnegative(),
});
export type LoginResponse = z.infer<typeof loginResponseSchema>;

/** What the BFF reports back to the browser about the session. */
export const sessionSchema = z.object({
  authenticated: z.boolean(),
  username: z.string().optional(),
  /** ADR 0010 — absent for a session predating it; treat as a regular user. */
  role: z.enum(["USER", "ADMIN"]).optional(),
  expiresAt: z.number().optional(),
});
export type Session = z.infer<typeof sessionSchema>;
