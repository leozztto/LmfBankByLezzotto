# 0010 — Autorização por escopo de conta: admin vê tudo, usuário comum só a própria

- **Status:** Aceito
- **Data:** 2026-09-11
- **Relacionado:** [0009](0009-validacao-real-de-credenciais-no-login.md)

## Contexto

A ADR 0009 deu ao login credenciais reais, mas nenhuma noção de **autorização**: uma vez
autenticado, qualquer usuário podia ler/operar qualquer conta pelo id — `GET /accounts`
inclusive documentava isso explicitamente ("no pagination — demo scope"). Faltava a
distinção pedida: um usuário **admin** com acesso a tudo, e usuários comuns restritos ao
**escopo da própria conta**.

Decisões fechadas antes de implementar (a mudança quebraria fluxos existentes se erradas):

1. **Cardinalidade usuário↔conta: 1 para 1.** Cada login tem no máximo uma conta vinculada.
2. **Vínculo é manual/admin, nunca automático.** `POST /accounts` continua exatamente como
   era — sem exigir que quem abre a conta esteja logado como o dono dela. Um admin vincula
   depois, explicitamente.
3. **Transferência quebraria com escopo ingênuo.** `GET /accounts` alimentava o dropdown de
   "conta de destino" na tela de transferência — restringi-lo à própria conta impediria
   transferir para qualquer outra pessoa, o próprio propósito da funcionalidade. Resolvido
   assim: **usuário comum digita o número da conta de destino** (resolvido por um lookup
   cross-account, sem restrição de dono); **admin mantém o dropdown** com a listagem
   completa.

## Decisão

### Backend

- **`app_user` ganha `role` (`USER`/`ADMIN`) e `account_id`** (nullable, unique — 1:1 com
  `Account`) — `V3__app_user_roles_and_account_link.sql`. Semeia um usuário `admin` (ADMIN).
  `demo` (ADR 0009) vira o exemplo de usuário comum, sem conta vinculada até um admin usar
  o novo endpoint.
- **JWT ganha as claims `role` e `accountId`** (`JwtService`). `JwtAuthenticationFilter`
  decodifica as duas e popula o `Authentication` com um `AuthenticatedUser(username, role,
  accountId)` como principal e a authority `ROLE_<role>` — não mais só o username.
- **`AccountAccessGuard.assertOwnerOrAdmin(accountId)`**: passa para admin; para usuário
  comum, só se `accountId` bater com o da própria sessão (`ForbiddenActionException` → `403
  FORBIDDEN` caso contrário). Aplicado em:
  - `AccountController` — `findById`, `findByDocumentNumber`, e `list()` (usuário comum
    recebe uma lista com 0 ou 1 conta — a sua, se vinculada; admin recebe `findAll()`
    inalterado);
  - `TransactionController.create` — no `accountId` da transação;
  - `TransferController.create` — **só no `fromAccountId`**; `toAccountId` fica de fora de
    propósito (transferir para a conta de outra pessoa não é violação de escopo);
  - `BankStatementController.getStatement` — no `accountId` do extrato.
- **`GET /accounts/number/{accountNumber}`** (`AccountLookupResponse`: só `accountId`,
  `accountNumber`, `fullName`) — cross-account e **sem** `AccountAccessGuard` de propósito:
  é o que resolve o destino de uma transferência para um usuário comum. Deliberadamente
  não expõe saldo/documento/endereço/status de quem não é o dono.
- **`PATCH /admin/users/{username}/account`** (`AdminUserController`, `AppUserService`):
  vincula uma conta a um usuário. Restrito a `ROLE_ADMIN` estaticamente em
  `SecurityConfig` (`.requestMatchers("/admin/**").hasRole("ADMIN")`) — diferente do
  `AccountAccessGuard`, que é dinâmico (compara ids em runtime), essa checagem é só padrão
  de URL, então fica no filtro de segurança, não em código de controller.
- ITs que testam regra de negócio (não autorização) — `AccountRestIT`, `MovementRestIT`,
  `BankStatementRestIT`, `BackendContractVerificationIT` — usam token `ADMIN` de propósito:
  eles criam a conta que operam na hora, então nunca seriam "donos" dela sob o novo modelo.
  A autorização em si tem sua própria suíte: `AccountAuthorizationIT`.

### Frontend

- `Session` (`lib/schemas/auth.ts`) ganha `role` opcional; `api/auth/login` e
  `api/auth/session` passam a devolver a claim `role` do JWT decodificado.
  `selectIsAdmin` (`stores/auth-store.ts`) — ausência de `role` (sessão anterior à ADR
  0010) é tratada como usuário comum.
- `TransferForm`: campo "Conta de destino" é `AccountSelect` (dropdown, como antes) só para
  admin; para usuário comum vira `AccountNumberInput` — digita o número, resolve via `GET
  /accounts/number/{n}` e mostra o nome do destinatário antes de confirmar.
- Nenhuma outra tela muda: `GET /accounts` já vem escopado pelo backend, então a listagem
  de contas, o seletor de origem no depósito/saque e o extrato simplesmente mostram só o
  que o usuário tem acesso, sem lógica nova no front.

## Consequências

- Um usuário comum sem conta vinculada (`app_user.account_id is null`) não vê **nenhuma**
  conta — `GET /accounts` devolve `[]`, e qualquer id específico dá `403`. É o estado
  inicial de `demo` até um admin rodar o `PATCH /admin/users/demo/account`.
- Abrir conta (`POST /accounts`) continua desacoplado de identidade — a ADR não resolve
  "toda conta tem um dono logado desde o início"; só passa a permitir restringir o acesso
  depois que o vínculo é feito.
- Não há tela de admin no frontend para o vínculo usuário↔conta — só o endpoint. Fica para
  quando (se) fizer sentido um painel administrativo de verdade.
- Novo teste de contrato (`GET /accounts/number/{accountNumber}`, sucesso e 404) e novos
  states (`an account exists with a known number`, `no account exists with a given
  number`) em `BackendContractVerificationIT`.
