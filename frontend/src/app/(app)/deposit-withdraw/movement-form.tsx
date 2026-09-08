"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";

import { maskCurrency } from "@/lib/masks/currency";
import { parseMoney } from "@/lib/schemas/common";
import {
  transactionFormSchema,
  type TransactionFormValues,
} from "@/lib/schemas/movement";
import { useMovement } from "@/hooks/use-movement";
import { useStatement } from "@/hooks/use-statement";
import { useUiStore } from "@/stores/ui-store";
import { formatBRL } from "@/lib/masks/currency";
import { Button } from "@/components/ui/button";
import { AccountSelect } from "@/components/account-select";
import { MaskedInput } from "@/components/masked-input";
import { Money } from "@/components/money";
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
import { Input } from "@/components/ui/input";
import {
  Tabs,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";
import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function MovementForm() {
  const selectedAccountId = useUiStore((s) => s.selectedAccountId);
  const setSelectedAccount = useUiStore((s) => s.setSelectedAccount);
  const movement = useMovement();
  const [confirmOpen, setConfirmOpen] = useState(false);

  const form = useForm<TransactionFormValues>({
    resolver: zodResolver(transactionFormSchema),
    mode: "onBlur",
    defaultValues: {
      accountId: selectedAccountId ?? 0,
      type: "C",
      amount: "",
      description: "",
    },
  });

  useEffect(() => {
    if (selectedAccountId) form.setValue("accountId", selectedAccountId);
  }, [selectedAccountId, form]);

  const accountId = form.watch("accountId");
  const type = form.watch("type");
  const amount = form.watch("amount");
  const statement = useStatement(accountId);

  function submit() {
    setConfirmOpen(false);
    movement.mutate(form.getValues(), {
      onSuccess: () => form.reset({ ...form.getValues(), amount: "", description: "" }),
    });
  }

  const parsedAmount = parseMoney(amount || "0");

  return (
    <>
      <Card>
        <CardHeader>
          <CardDescription>Saldo da conta</CardDescription>
          <CardTitle className="text-2xl">
            <Money value={statement.data?.balance ?? "0"} />
          </CardTitle>
        </CardHeader>
      </Card>

      <Form {...form}>
        <form
          className="space-y-4"
          onSubmit={form.handleSubmit(() => setConfirmOpen(true))}
        >
          <ApiErrorAlert error={movement.error} />

          <FormField
            control={form.control}
            name="type"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Operação</FormLabel>
                <Tabs
                  value={field.value}
                  onValueChange={(v) => field.onChange(v as "C" | "D")}
                >
                  <TabsList>
                    <TabsTrigger value="C">Depósito</TabsTrigger>
                    <TabsTrigger value="D">Saque</TabsTrigger>
                  </TabsList>
                </Tabs>
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="accountId"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Conta</FormLabel>
                <AccountSelect
                  aria-label="Conta"
                  value={field.value || null}
                  onChange={(id) => {
                    field.onChange(id);
                    setSelectedAccount(id);
                  }}
                />
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
                      movement.resetKey();
                    }}
                    placeholder="R$ 0,00"
                  />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <FormField
            control={form.control}
            name="description"
            render={({ field }) => (
              <FormItem>
                <FormLabel>Descrição</FormLabel>
                <FormControl>
                  <Input {...field} placeholder="Ex.: aluguel" />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />

          <Button type="submit" disabled={movement.isPending}>
            {movement.isPending
              ? "Processando…"
              : type === "C"
                ? "Depositar"
                : "Sacar"}
          </Button>
        </form>
      </Form>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title={type === "C" ? "Confirmar depósito" : "Confirmar saque"}
        description={`${type === "C" ? "Depositar" : "Sacar"} ${formatBRL(parsedAmount)} nesta conta?`}
        confirmLabel={type === "C" ? "Depositar" : "Sacar"}
        loading={movement.isPending}
        onConfirm={submit}
      />
    </>
  );
}
