"use client";

import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { createAccount } from "@/lib/api/accounts";
import type { AccountCreatePayload } from "@/lib/schemas/account";
import { queryKeys } from "@/lib/query/keys";
import { notify } from "@/lib/notify";

export function useCreateAccount() {
  const router = useRouter();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: AccountCreatePayload) => createAccount(payload),
    onSuccess: async (account) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.accounts.all });
      notify.success(`Conta ${account.accountNumber} criada`);
      router.push(`/accounts/${account.accountId}`);
    },
  });
}
