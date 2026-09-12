"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import {
  createUserFormSchema,
  type CreateUserFormValues,
} from "@/lib/schemas/admin";
import { applyFieldErrors } from "@/lib/forms";
import { useCreateUser } from "@/hooks/use-admin-users";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { ApiErrorAlert } from "@/components/api-error-alert";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

/** Sempre cria role USER, sem conta vinculada — ADR 0010. */
export function CreateUserForm() {
  const mutation = useCreateUser();

  const form = useForm<CreateUserFormValues>({
    resolver: zodResolver(createUserFormSchema),
    defaultValues: { username: "", password: "" },
  });

  function submit(values: CreateUserFormValues) {
    mutation.mutate(values, {
      onSuccess: () => form.reset({ username: "", password: "" }),
      onError: (error) => applyFieldErrors(error, form.setError),
    });
  }

  return (
    <Form {...form}>
      <form className="space-y-4" onSubmit={form.handleSubmit(submit)}>
        <ApiErrorAlert error={mutation.error} />

        <FormField
          control={form.control}
          name="username"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Usuário</FormLabel>
              <FormControl>
                <Input autoComplete="off" {...field} />
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
                <Input type="password" autoComplete="new-password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Criando…" : "Criar usuário"}
        </Button>
      </form>
    </Form>
  );
}
