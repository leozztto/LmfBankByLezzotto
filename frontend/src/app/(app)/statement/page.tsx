import { StatementTable } from "./statement-table";

export default function StatementPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Extrato</h1>
        <p className="text-sm text-muted-foreground">
          Saldo e lançamentos, com filtro por período.
        </p>
      </div>
      <StatementTable />
    </div>
  );
}
