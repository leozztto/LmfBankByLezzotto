"use client";

import { useMutation } from "@tanstack/react-query";

import { createUser, linkAccount } from "@/lib/api/admin";
import type {
  CreateUserFormValues,
  LinkAccountFormValues,
} from "@/lib/schemas/admin";
import { notify } from "@/lib/notify";

export function useCreateUser() {
  return useMutation({
    mutationFn: (values: CreateUserFormValues) => createUser(values),
    onSuccess: (user) => {
      notify.success(`Usuário ${user.username} criado`);
    },
  });
}

export function useLinkAccount() {
  return useMutation({
    mutationFn: (values: LinkAccountFormValues) =>
      linkAccount(values.username, values.accountId),
    onSuccess: (_data, values) => {
      notify.success(
        `Conta ${values.accountId} vinculada a ${values.username}`,
      );
    },
  });
}
