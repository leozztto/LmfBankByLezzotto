import { TransferForm } from "./transfer-form";

export default function TransferPage() {
  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Transferência</h1>
        <p className="text-sm text-muted-foreground">
          Entre contas registradas.
        </p>
      </div>
      <TransferForm />
    </div>
  );
}
