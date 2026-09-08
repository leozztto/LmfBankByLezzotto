"use client";

import { useRef } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createTransfer } from "@/lib/api/movements";
import { TransferFailedError } from "@/lib/api/errors";
import {
  toTransferPayload,
  type TransferFormValues,
} from "@/lib/schemas/movement";
import { queryKeys } from "@/lib/query/keys";
import { notify } from "@/lib/notify";

/**
 * Transfer. One idempotency UUID per attempt (reused on retry — the backend
 * replay is safe). A `201` that comes back `status: "FAILED"` is surfaced as a
 * `TransferFailedError`, not a success.
 */
export function useTransfer() {
  const queryClient = useQueryClient();
  const keyRef = useRef(crypto.randomUUID());

  const mutation = useMutation({
    mutationFn: async (values: TransferFormValues) => {
      const res = await createTransfer(
        toTransferPayload(values, keyRef.current),
      );
      if (res.status === "FAILED") {
        throw new TransferFailedError(res.failureReason);
      }
      return res;
    },
    onSuccess: async (res) => {
      keyRef.current = crypto.randomUUID();
      for (const id of [res.fromAccountId, res.toAccountId]) {
        await queryClient.invalidateQueries({ queryKey: ["statement", id] });
        await queryClient.invalidateQueries({
          queryKey: queryKeys.accounts.detail(id),
        });
      }
      await queryClient.invalidateQueries({
        queryKey: queryKeys.accounts.list(),
      });
      notify.success("Transferência concluída");
    },
  });

  const resetKey = () => {
    keyRef.current = crypto.randomUUID();
  };

  return { ...mutation, resetKey };
}
