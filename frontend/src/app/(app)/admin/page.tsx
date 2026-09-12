import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { SESSION_COOKIE } from "@/lib/auth/constants";
import { decodeJwt } from "@/lib/auth/jwt";
import { CreateUserForm } from "./create-user-form";
import { LinkAccountForm } from "./link-account-form";

/**
 * Só ADMIN (ADR 0010). `(app)/layout.tsx` já garante sessão válida; aqui só falta
 * checar o papel — mesma leitura de cookie/JWT que o layout faz, sem depender do
 * `useAuthStore` (evita um flash de conteúdo antes da hidratação no cliente).
 */
export default function AdminPage() {
  const token = cookies().get(SESSION_COOKIE)?.value;
  const role = token ? decodeJwt(token)?.role : undefined;

  if (role !== "ADMIN") {
    redirect("/dashboard");
  }

  return (
    <div className="mx-auto max-w-lg space-y-10">
      <div>
        <h1 className="text-2xl font-semibold">Admin</h1>
        <p className="text-sm text-muted-foreground">
          Criar logins e vincular contas (ADR 0010) — sem cadastro automático na
          abertura de conta.
        </p>
      </div>

      <section className="space-y-4">
        <h2 className="text-lg font-medium">Criar usuário</h2>
        <CreateUserForm />
      </section>

      <section className="space-y-4">
        <h2 className="text-lg font-medium">Vincular conta</h2>
        <LinkAccountForm />
      </section>
    </div>
  );
}
