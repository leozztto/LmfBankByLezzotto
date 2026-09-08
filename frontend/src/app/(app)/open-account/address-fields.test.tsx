import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { FormProvider, useForm } from "react-hook-form";
import { describe, expect, it } from "vitest";

import { emptyAddress, type AccountFormValues } from "@/lib/schemas/account";
import { Form } from "@/components/ui/form";
import { AddressFields } from "./address-fields";

function Harness({ initial = 1 }: { initial?: number }) {
  const form = useForm<AccountFormValues>({
    defaultValues: {
      addresses: Array.from({ length: initial }, () => ({ ...emptyAddress })),
    } as Partial<AccountFormValues> as AccountFormValues,
  });
  return (
    <FormProvider {...form}>
      <Form {...form}>
        <AddressFields />
      </Form>
    </FormProvider>
  );
}

describe("AddressFields", () => {
  it("renders one address block by default without a remove button", () => {
    render(<Harness />);
    expect(screen.getByText("Endereço 1")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Remover" }),
    ).not.toBeInTheDocument();
  });

  it("adds a second block, shows remove buttons, then removes one", async () => {
    const user = userEvent.setup();
    render(<Harness />);

    await user.click(
      screen.getByRole("button", { name: "Adicionar endereço" }),
    );
    expect(screen.getByText("Endereço 2")).toBeInTheDocument();

    const removeButtons = screen.getAllByRole("button", { name: "Remover" });
    expect(removeButtons).toHaveLength(2);

    await user.click(removeButtons[0]!);
    expect(screen.queryByText("Endereço 2")).not.toBeInTheDocument();
  });

  it("renders every field for an address block", () => {
    render(<Harness />);
    for (const label of [
      "CEP",
      "Logradouro",
      "Bairro",
      "Número",
      "Complemento",
      "Cidade",
      "UF",
      "Tipo",
    ]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });
});
