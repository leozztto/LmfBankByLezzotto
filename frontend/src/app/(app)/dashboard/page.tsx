import { DashboardSummary } from "./dashboard-summary";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Painel</h1>
        <p className="text-sm text-muted-foreground">
          Fluxo bancário — abrir conta, movimentar e consultar extrato.
        </p>
      </div>
      <DashboardSummary />
    </div>
  );
}
