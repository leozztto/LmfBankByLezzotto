import { AccountsTable } from "./accounts-table";
import { DocumentSearch } from "./document-search";

export default function AccountsPage() {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Contas</h1>
          <p className="text-sm text-muted-foreground">
            Todas as contas registradas.
          </p>
        </div>
        <DocumentSearch />
      </div>
      <AccountsTable />
    </div>
  );
}
