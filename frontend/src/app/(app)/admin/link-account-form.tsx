"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import {
  linkAccountFormSchema,
  type LinkAccountFormValues,
} from "@/lib/schemas/admin";
import { useLinkAccount } from "@/hooks/use-admin-users";
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

/** Vincula uma conta já existente a um login já existente — nunca automático (ADR 0010). */
export function LinkAccountForm() {
  const mutation = useLinkAccount();

  const form = useForm<LinkAccountFormValues>({
    resolver: zodResolver(linkAccountFormSchema),
    defaultValues: { username: "", accountId: 0 },
  });

  function submit(values: LinkAccountFormValues) {
    mutation.mutate(values, {
      onSuccess: () => form.reset({ username: "", accountId: 0 }),
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
              <FormLabel>Usuário a vincular</FormLabel>
              <FormControl>
                <Input autoComplete="off" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="accountId"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Id da conta</FormLabel>
              <FormControl>
                <Input
                  type="number"
                  min={1}
                  value={field.value || ""}
                  onChange={(e) => field.onChange(Number(e.target.value))}
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? "Vinculando…" : "Vincular conta"}
        </Button>
      </form>
    </Form>
  );
}
