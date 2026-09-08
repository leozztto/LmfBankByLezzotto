import { http, HttpResponse } from "msw";
import userEvent from "@testing-library/user-event";
import { screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

import { server } from "@/test/msw/server";
import { renderWithProviders } from "@/test/utils";
import { AccountForm } from "./account-form";

const push = vi.fn();
vi.mock("next/navigation", () => ({ useRouter: () => ({ push }) }));
vi.mock("@/lib/notify", () => ({
  notify: { success: vi.fn(), error: vi.fn(), info: vi.fn() },
}));

const createdAccount = (id: number) => ({
  accountId: id,
  fullName: "Maria Silva",
  maskedDocument: "***.982.***-25",
  maskedEmail: "ma***@example.com",
  maskedPhone: "(11) *****-4321",
  accountType: "C",
  accountNumber: "12345678-9",
  agency: "0001",
  accountStatus: "A",
  createdAt: "2026-09-08T10:00:00",
  updatedAt: null,
  balance: { availableBalance: "0", blockedBalance: "0", totalBalance: "0" },
  addresses: [],
});

async function fillValidForm() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("Nome completo"), "Maria Silva");
  await user.type(screen.getByLabelText("CPF"), "52998224725");
  await user.type(screen.getByLabelText("Data de nascimento"), "1990-05-20");
  await user.type(screen.getByLabelText("Nome da mãe"), "Joana Silva");
  await user.type(screen.getByLabelText("E-mail"), "maria@example.com");
  await user.type(screen.getByLabelText("Telefone"), "11987654321");
  await user.type(screen.getByLabelText("Profissão"), "Engenheira");
  await user.type(screen.getByLabelText("Renda mensal"), "1200000");

  const addr = screen.getByText("Endereço 1").closest("div")!.parentElement!;
  await user.type(within(addr).getByLabelText("CEP"), "01001000");
  await user.type(within(addr).getByLabelText("Logradouro"), "Praça da Sé");
  await user.type(within(addr).getByLabelText("Bairro"), "Sé");
  await user.type(within(addr).getByLabelText("Número"), "100");
  await user.type(within(addr).getByLabelText("Cidade"), "São Paulo");
  await user.type(within(addr).getByLabelText("UF"), "SP");

  await user.click(screen.getByRole("checkbox"));
  return user;
}

describe("AccountForm", () => {
  beforeEach(() => push.mockClear());

  it("blocks an empty submit and does not call the API", async () => {
    let called = false;
    server.use(
      http.post("/api/accounts", () => {
        called = true;
        return HttpResponse.json({}, { status: 201 });
      }),
    );

    renderWithProviders(<AccountForm />);
    await userEvent.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(
      await screen.findByText("Informe o nome completo"),
    ).toBeInTheDocument();
    expect(called).toBe(false);
  });

  it("submits a payload without createdAt/agency/accountStatus and navigates", async () => {
    let body: Record<string, unknown> | undefined;
    server.use(
      http.post("/api/accounts", async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(createdAccount(42), { status: 201 });
      }),
    );

    renderWithProviders(<AccountForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/accounts/42"));
    expect(body).toMatchObject({
      documentNumber: "52998224725",
      monthlyIncome: 12000,
      accountType: "C",
      acceptedTerms: true,
    });
    expect(body).not.toHaveProperty("createdAt");
    expect(body).not.toHaveProperty("agency");
    expect(body).not.toHaveProperty("accountStatus");
  });

  it("shows a 409 inline", async () => {
    server.use(
      http.post("/api/accounts", () =>
        HttpResponse.json(
          {
            status: 409,
            code: "DOCUMENT_ALREADY_EXISTS",
            message: "documento já possui conta",
          },
          { status: 409 },
        ),
      ),
    );

    renderWithProviders(<AccountForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(
      await screen.findByText("documento já possui conta"),
    ).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });

  it("maps backend fieldErrors onto the fields", async () => {
    server.use(
      http.post("/api/accounts", () =>
        HttpResponse.json(
          {
            status: 400,
            code: "VALIDATION_ERROR",
            message: "Falha de validação",
            fieldErrors: { email: "endereço de e-mail inválido" },
          },
          { status: 400 },
        ),
      ),
    );

    renderWithProviders(<AccountForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "Criar conta" }));

    expect(
      await screen.findByText("endereço de e-mail inválido"),
    ).toBeInTheDocument();
  });

  it("adds and removes address rows", async () => {
    renderWithProviders(<AccountForm />);
    const user = userEvent.setup();

    expect(screen.queryByText("Endereço 2")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Adicionar endereço" }));
    expect(screen.getByText("Endereço 2")).toBeInTheDocument();

    await user.click(screen.getAllByRole("button", { name: "Remover" })[0]);
    expect(screen.queryByText("Endereço 2")).not.toBeInTheDocument();
  });
});
