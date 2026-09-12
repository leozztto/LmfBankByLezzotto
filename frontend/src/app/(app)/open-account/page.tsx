import { AccountForm } from "./account-form";

export default function OpenAccountPage() {
  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Abrir conta</h1>
        <p className="text-sm text-muted-foreground">
          Preencha os dados do titular e ao menos um endereço.
        </p>
      </div>
      <AccountForm />
    </div>
  );
}
