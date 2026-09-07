"use client";

import { useEffect, useState } from "react";

// Placeholder da Fase 1: prova que a rota front -> nginx -> backend funciona
// na mesma origem (ADR 0003). As telas reais são a Fase 3.
export default function Home() {
  const [status, setStatus] = useState("checking…");

  useEffect(() => {
    fetch("/api/actuator/health")
      .then((r) => r.json())
      .then((d) => setStatus(d.status ?? "unknown"))
      .catch(() => setStatus("unreachable"));
  }, []);

  return (
    <main className="flex min-h-screen flex-col items-center justify-center gap-2">
      <h1 className="text-2xl font-bold">LmfBank</h1>
      <p data-testid="health">backend health: {status}</p>
    </main>
  );
}
