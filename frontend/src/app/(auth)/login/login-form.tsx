"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { login } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/errors";
import { loginSchema, type LoginInput } from "@/lib/schemas/auth";
import { queryKeys } from "@/lib/query/keys";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Alert, AlertDescription } from "@/components/ui/alert";

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const queryClient = useQueryClient();
  const setSession = useAuthStore((s) => s.setSession);

  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: (input: LoginInput) => login(input),
    // Um 401 aqui é "credenciais erradas", não "sessão expirou" — sem isso, o
    // handler global (lib/query/client.ts) faria um hard-navigate pra /login
    // (útil pra sessão que expirou noutra tela) que apagaria este form antes
    // de ele conseguir mostrar o erro (ADR 0009).
    meta: { skipGlobalErrorHandler: true },
    onSuccess: async (session) => {
      setSession(session);
      await queryClient.invalidateQueries({ queryKey: queryKeys.session });
      const next = params.get("next");
      router.replace(next && next.startsWith("/") ? next : "/dashboard");
    },
  });

  const errorMessage =
    mutation.error instanceof ApiError
      ? mutation.error.status === 401
        ? "Usuário ou senha inválidos"
        : mutation.error.message
      : mutation.isError
        ? "Não foi possível entrar. Tente novamente."
        : null;

  return (
    <Form {...form}>
      <form
        className="space-y-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      >
        {errorMessage && (
          <Alert variant="destructive">
            <AlertDescription>{errorMessage}</AlertDescription>
          </Alert>
        )}

        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Usuário</FormLabel>
              <FormControl>
                <Input autoComplete="username" autoFocus {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Senha</FormLabel>
              <FormControl>
                <Input
                  type="password"
                  autoComplete="current-password"
                  {...field}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Entrando…" : "Entrar"}
        </Button>
      </form>
    </Form>
  );
}
