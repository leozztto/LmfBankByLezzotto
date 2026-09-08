import { MovementForm } from "./movement-form";

export default function DepositWithdrawPage() {
  return (
    <div className="mx-auto max-w-lg space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Depósito / Saque</h1>
        <p className="text-sm text-muted-foreground">
          Movimente o saldo de uma conta.
        </p>
      </div>
      <MovementForm />
    </div>
  );
}
