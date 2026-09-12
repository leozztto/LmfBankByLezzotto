"use client";

import { useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { maskCurrency, formatBRL } from "@/lib/masks/currency";
import { parseMoney } from "@/lib/schemas/common";
import {
  transferFormSchema,
  type TransferFormValues,
} from "@/lib/schemas/movement";
import { useTransfer } from "@/hooks/use-transfer";
import { useUiStore } from "@/stores/ui-store";
import { useAuthStore, selectIsAdmin } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { AccountSelect } from "@/components/account-select";
import { AccountNumberInput } from "@/components/account-number-input";
import { MaskedInput } from "@/components/masked-input";
import { ApiErrorAlert } from "@/components/api-error-alert";
import { ConfirmDialog } from "@/components/confirm-dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

export function TransferForm() {
  const selectedAccountId = useUiStore((s) => s.selectedAccountId);
  const isAdmin = useAuthStore(selectIsAdmin);
  const transfer = useTransfer();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const form = useForm<TransferFormValues>({
    resolver: zodResolver(transferFormSchema),
    mode: "onBlur",
    defaultValues: {
      fromAccountId: selectedAccountId ?? 0,
      toAccountId: 0,
      amount: "",
    },
  });

  const from = form.watch("fromAccountId");
  const amount = form.watch("amount");

  function submit() {
    setConfirmOpen(false);
    transfer.mutate(form.getValues(), {
      onSuccess: () => form.reset({ ...form.getValues(), amount: "" }),
    });
  }

  return (
    <>
      <Form {...form}>
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit(() => setConfirmOpen(true))}
        >
          <ApiErrorAlert error={transfer.error} />

          <FormField
            control={form.control}
            name="fromAccountId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Conta de origem</FormLabel>
                <AccountSelect
                  aria-label="Conta de origem"
                  value={field.value || null}
                  onChange={field.onChange}
                />
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="toAccountId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Conta de destino</FormLabel>
                {isAdmin ? (
                  <AccountSelect
                    aria-label="Conta de destino"
                    value={field.value || null}
                    onChange={field.onChange}
                    exclude={from || null}
                    placeholder="Selecione o destino"
                  />
                ) : (
                  <AccountNumberInput
                    aria-label="Conta de destino"
                    value={field.value || null}
                    onChange={field.onChange}
                  />
                )}
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="amount"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Valor</FormLabel>
                <FormControl>
                  <MaskedInput
                    mask={maskCurrency}
                    value={field.value}
                    onChange={(v) => {
                      field.onChange(v);
                      transfer.resetKey();
                    }}
                    placeholder="R$ 0,00"
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <Button type="submit" disabled={transfer.isPending}>
            {transfer.isPending ? "Transferindo…" : "Transferir"}
          </Button>
        </form>
      </Form>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Confirmar transferência"
        description={`Transferir ${formatBRL(parseMoney(amount || "0"))} para a conta de destino?`}
        confirmLabel="Transferir"
        loading={transfer.isPending}
        onConfirm={submit}
      />
    </>
  );
}
