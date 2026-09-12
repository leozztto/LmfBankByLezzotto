import { fileURLToPath } from "node:url";

import { PactV3 } from "@pact-foundation/pact";

/**
 * Nomes dos pacticipants — precisam bater com o `@Provider("lmfbank-backend")` do
 * backend e com os `--pacticipant` usados no `pact-broker` (CI).
 */
export const CONSUMER = "lmfbank-frontend";
export const PROVIDER = "lmfbank-backend";

/** Onde os pacts gerados são escritos (git-ignored, publicados pelo CI). */
export const PACTS_DIR = fileURLToPath(new URL("../pacts", import.meta.url));

/** Um `PactV3` novo por arquivo de teste; múltiplos `executeTest` acumulam no mesmo pact. */
export function newPact(): PactV3 {
  return new PactV3({
    consumer: CONSUMER,
    provider: PROVIDER,
    dir: PACTS_DIR,
  });
}

/**
 * O backend aceita qualquer JWT assinado com a chave dele. O provider (verificação)
 * injeta um `Authorization: Bearer <token real>` em toda requisição, então aqui só
 * declaramos que o header EXISTE no formato `Bearer <algo>`. Este é o valor de
 * exemplo que o BFF envia durante o teste de consumer.
 */
export const BEARER_EXAMPLE =
  "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vIn0.sig";
