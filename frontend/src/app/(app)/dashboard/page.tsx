import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function DashboardPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">Painel</h1>
        <p className="text-sm text-muted-foreground">
          Fluxo bancário — abrir conta, movimentar e consultar extrato.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Bem-vindo</CardTitle>
          <CardDescription>
            As telas do fluxo completo chegam nos próximos PRs da Fase 3.
          </CardDescription>
        </CardHeader>
        <CardContent className="text-sm text-muted-foreground">
          Use o menu à esquerda para navegar.
        </CardContent>
      </Card>
    </div>
  );
}
