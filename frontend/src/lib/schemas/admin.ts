import { z } from "zod";

/** Admin-only user management (ADR 0010). */

export const createUserFormSchema = z.object({
  username: z.string().min(1, "Informe o usuário"),
  password: z.string().min(1, "Informe a senha"),
});
export type CreateUserFormValues = z.infer<typeof createUserFormSchema>;

export const linkAccountFormSchema = z.object({
  username: z.string().min(1, "Informe o usuário"),
  accountId: z.number().int().positive("Informe o id da conta"),
});
export type LinkAccountFormValues = z.infer<typeof linkAccountFormSchema>;

/** POST /admin/users response — never carries the password hash. */
export const appUserResponseSchema = z
  .object({
    username: z.string(),
    role: z.enum(["USER", "ADMIN"]),
    accountId: z.number().nullable().optional(),
  })
  .passthrough();
export type AppUserResponse = z.infer<typeof appUserResponseSchema>;
