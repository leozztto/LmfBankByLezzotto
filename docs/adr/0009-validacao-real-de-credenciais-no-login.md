# 0009 — Validação real de credenciais em `POST /auth/login`

- **Status:** Aceito
- **Data:** 2026-09-11
- **Relacionado:** [0007](0007-token-jwt-cookie-httponly-bff-next.md)

## Contexto

Desde a Fase 3.1, `POST /auth/login` (`AuthController`) recebia `username` e `password`,
mas só usava o `username` para assinar o JWT — a senha nunca era comparada com nada. A
[ADR 0007](0007-token-jwt-cookie-httponly-bff-next.md) já registrava isso como decisão
consciente da época: *"o login não tem user store (`sub` é um username arbitrário,
qualquer senha passa) — não há vínculo usuário↔conta a expor"*. Fazia sentido enquanto o
foco era construir o fluxo bancário (abrir conta, depósito, transferência, extrato), que
não depende de identidade real do usuário logado.

Isso deixou de ser aceitável como comportamento observável do produto: qualquer
usuário/senha (ex.: `1`/`1`) autentica com sucesso e recebe um JWT válido, o que não é o
que uma tela de login deveria fazer, mesmo num projeto de demonstração.

Forças em jogo:

- Não existe tela de cadastro de usuário de login — abrir conta bancária (`POST
  /accounts`) é um fluxo separado e não coleta senha.
- O front-end (`frontend/pact/auth.pact.test.ts`, `LoginForm`) e o BFF (`api/auth/login/
  route.ts`) **já** tratavam um `401` do backend como caminho possível — o BFF repassa o
  status e o `LoginForm` já traduzia `401` para "Usuário ou senha inválidos". A lacuna
  era só o backend nunca emitir esse `401`.
- `spring-boot-starter-security` já traz `spring-security-crypto` (BCrypt) transitivamente
  — não é preciso nova dependência.
- Mantenedor único, sem necessidade de auto-cadastro de usuários de login.

## Decisão

Adotamos validação real de usuário/senha, com um usuário de demonstração semeado por
migration — sem criar uma tela de cadastro de usuário (fora do escopo do projeto):

- Tabela `app_user` (`V2__app_user.sql`): `id`, `username` (unique), `password_hash`,
  `created_at`. Semeia um único registro: `demo` / hash BCrypt de `demo`.
- Entidade `AppUser` + `AppUserRepository.findByUsername`.
- `AuthService.authenticate(username, password)`: busca o usuário e compara a senha com
  `PasswordEncoder.matches` (bean `BCryptPasswordEncoder` em `SecurityConfig`); lança
  `InvalidCredentialsException` tanto para usuário inexistente quanto para senha errada —
  **mesma mensagem e status nos dois casos**, para não revelar quais usernames existem.
- `AuthController.login` chama `authService.authenticate(...)` antes de gerar o token;
  `GlobalExceptionHandler` mapeia `InvalidCredentialsException` para `401
  INVALID_CREDENTIALS` (mesmo formato `ApiError` das demais exceções de negócio).
- Contrato (ADR 0008): novo estado `"credentials are rejected"` em
  `BackendContractVerificationIT` + interação correspondente em `auth.pact.test.ts`
  (senha errada → `401`), ao lado do já existente `"credentials are accepted"`.
- E2E (`e2e/fixtures/flows.ts`, `login.spec.ts`): credenciais padrão trocadas de
  `e2e`/`e2e` (usuário fictício que não existe mais) para `demo`/`demo`; novo teste cobre
  o caminho de credenciais inválidas.

## Consequências

- `POST /auth/login` com usuário/senha errados agora responde `401 INVALID_CREDENTIALS`
  em vez de emitir um JWT válido para qualquer entrada.
- Para logar localmente, no E2E ou testar manualmente a stack, as credenciais são
  `demo` / `demo` (semeadas pela migration) — documentado aqui e no `README.md`.
- Ainda **não existe** tela de cadastro de usuário nem vínculo entre `app_user` e
  `Account` — continua sendo um login "genérico", só que agora com credencial real.
  Login sem vínculo com conta bancária é aceitável para o escopo atual do projeto; criar
  esse vínculo (e uma tela de cadastro de usuário) fica para um ADR futuro, se necessário.
- A nota da [ADR 0007](0007-token-jwt-cookie-httponly-bff-next.md) ("qualquer senha
  passa") fica desatualizada por este ADR — o restante da 0007 (cookie httpOnly, BFF,
  ausência de CSRF) continua válido e não é afetado.
