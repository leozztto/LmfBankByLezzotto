"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Search } from "lucide-react";

import { maskCpf } from "@/lib/masks/cpf";
import { onlyDigits } from "@/lib/schemas/common";
import { isValidCpf } from "@/lib/schemas/common";
import { useAccountByDocumentQuery } from "@/hooks/use-accounts";
import { ApiError } from "@/lib/api/errors";
import { Button } from "@/components/ui/button";
import { MaskedInput } from "@/components/masked-input";

export function DocumentSearch() {
  const router = useRouter();
  const [value, setValue] = useState("");
  const [submitted, setSubmitted] = useState("");

  const digits = onlyDigits(submitted);
  const query = useAccountByDocumentQuery(submitted);

  if (query.data) {
    router.push(`/accounts/${query.data.accountId}`);
  }

  const notFound =
    query.isError &&
    query.error instanceof ApiError &&
    query.error.code === "ACCOUNT_NOT_FOUND";

  const invalid = value.length > 0 && !isValidCpf(value);

  return (
    <form
      className="flex flex-wrap items-start gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        if (isValidCpf(value)) setSubmitted(value);
      }}
    >
      <div>
        <MaskedInput
          mask={maskCpf}
          value={value}
          onChange={setValue}
          placeholder="Buscar por CPF"
          className="w-52"
          aria-label="CPF"
        />
        {invalid && (
          <p className="mt-1 text-xs text-destructive">CPF inválido</p>
        )}
        {notFound && digits.length === 11 && (
          <p className="mt-1 text-xs text-destructive">
            Nenhuma conta para esse CPF
          </p>
        )}
      </div>
      <Button type="submit" variant="secondary" disabled={query.isFetching}>
        <Search />
        Buscar
      </Button>
    </form>
  );
}
