"use client";

import { useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createTransaction } from "@/lib/api/movements";
import {
  toTransactionPayload,
  type TransactionFormValues,
} from "@/lib/schemas/movement";
import { queryKeys } from "@/lib/query/keys";
import { notify } from "@/lib/notify";

/**
 * Deposit / withdraw. Holds one idempotency key per "attempt": reused on a
 * retry, regenerated after a success or when the inputs change.
 */
export function useMovement() {
  const queryClient = useQueryClient();
  const keyRef = useRef(crypto.randomUUID());

  const mutation = useMutation({
    mutationFn: (values: TransactionFormValues) =>
      createTransaction(toTransactionPayload(values, keyRef.current)),
    onSuccess: async (txn, values) => {
      keyRef.current = crypto.randomUUID();
      await queryClient.invalidateQueries({
        queryKey: ["statement", values.accountId],
      });
      await queryClient.invalidateQueries({
        queryKey: queryKeys.accounts.detail(values.accountId),
      });
      notify.success(
        txn.type === "CREDIT" ? "Depósito realizado" : "Saque realizado",
      );
    },
  });

  /** Call before letting the user re-submit changed inputs. */
  const resetKey = () => {
    keyRef.current = crypto.randomUUID();
  };

  return { ...mutation, resetKey };
}
