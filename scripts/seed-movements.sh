#!/usr/bin/env bash
# Massa de teste: depósitos, saques e transferências entre as contas 1-8 (ou
# outras, via ACCOUNT_IDS), pra ter movimentação de verdade pra navegar no
# extrato/dashboard. Roda como admin (ADR 0010) — bypassa o escopo por conta,
# então não importa a quem cada conta esteja vinculada.
#
# Uso:
#   ./scripts/seed-movements.sh
#
# Variáveis de ambiente (todas opcionais):
#   BASE_URL      default http://localhost:8080 (backend direto, sem nginx/BFF)
#   ADMIN_USER    default admin
#   ADMIN_PASS    default admin
#   ACCOUNT_IDS   default "1 2 3 4 5 6 7 8"
#   DEPOSITS      default 130   (quantidade de depósitos aleatórios)
#   WITHDRAWALS   default 40    (quantidade de saques aleatórios)
#   TRANSFERS     default 32   (quantidade de transferências aleatórias)
#
# Requer: curl, node (só pra gerar UUID e ler o token — já é dependência do
# projeto).
#
# Nota Windows/Git Bash: o corpo de cada requisição é montado via heredoc e
# enviado com `--data-binary @arquivo`, nunca como argumento inline de `-d`.
# Um argumento de linha de comando com acento (UTF-8 multibyte) passado direto
# pro curl.exe nativo chega corrompido do outro lado — heredoc/arquivo não
# passa por essa conversão.

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"
read -ra ACCOUNTS <<< "${ACCOUNT_IDS:-1 2 3 4 5 6 7 8}"
DEPOSITS="${DEPOSITS:-12}"
WITHDRAWALS="${WITHDRAWALS:-8}"
TRANSFERS="${TRANSFERS:-10}"

DEPOSIT_DESCRIPTIONS=("Salário" "Bônus" "Reembolso" "PIX recebido" "Depósito em dinheiro" "Venda de item" "Cashback")
WITHDRAW_DESCRIPTIONS=("Compra no débito" "Saque em caixa eletrônico" "Conta de luz" "Assinatura" "Mercado" "Farmácia")

BODY_FILE=$(mktemp)
RESP_FILE=$(mktemp)
trap 'rm -f "$BODY_FILE" "$RESP_FILE"' EXIT

ok=0
fail=0

uuid() { node -pe "crypto.randomUUID()"; }

random_amount() {
  local min_int="$1" max_int="$2"
  local whole=$((RANDOM % (max_int - min_int + 1) + min_int))
  local cents=$((RANDOM % 100))
  printf "%d.%02d" "$whole" "$cents"
}

random_account() {
  echo "${ACCOUNTS[$((RANDOM % ${#ACCOUNTS[@]}))]}"
}

random_item() {
  local -n arr=$1
  echo "${arr[$((RANDOM % ${#arr[@]}))]}"
}

report() {
  local label="$1" status="$2"
  if [[ "$status" =~ ^2 ]]; then
    ok=$((ok + 1))
    echo "  ok   [$status] $label"
  else
    fail=$((fail + 1))
    echo "  FAIL [$status] $label -> $(cat "$RESP_FILE")"
  fi
}

# Lê o corpo JSON do stdin (heredoc) pro $BODY_FILE e faz o POST/PATCH
# autenticado. Devolve o status HTTP.
auth_post() {
  local path="$1"
  cat > "$BODY_FILE"
  curl -s -o "$RESP_FILE" -w "%{http_code}" -X POST \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    --data-binary "@$BODY_FILE" "$BASE_URL$path"
}

echo "==> logando como $ADMIN_USER em $BASE_URL"
LOGIN_RES=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}")
TOKEN=$(echo "$LOGIN_RES" | node -pe "JSON.parse(require('fs').readFileSync(0)).token" 2>/dev/null || true)

if [[ -z "${TOKEN:-}" || "$TOKEN" == "undefined" ]]; then
  echo "Não consegui logar como admin. Resposta: $LOGIN_RES" >&2
  exit 1
fi
echo "==> token ok"

do_deposit() {
  local accountId amount description status
  accountId=$(random_account)
  amount=$(random_amount 50 1000)
  description=$(random_item DEPOSIT_DESCRIPTIONS)
  status=$(auth_post /transactions <<EOF
{
  "accountId": $accountId,
  "type": "C",
  "amount": $amount,
  "description": "$description",
  "idempotencyKey": "$(uuid)"
}
EOF
  )
  report "depósito  conta=$accountId valor=R\$$amount ($description)" "$status"
}

do_withdraw() {
  local accountId amount description status
  accountId=$(random_account)
  amount=$(random_amount 10 150)
  description=$(random_item WITHDRAW_DESCRIPTIONS)
  status=$(auth_post /transactions <<EOF
{
  "accountId": $accountId,
  "type": "D",
  "amount": $amount,
  "description": "$description",
  "idempotencyKey": "$(uuid)"
}
EOF
  )
  report "saque     conta=$accountId valor=R\$$amount ($description)" "$status"
}

do_transfer() {
  local from to amount status
  from=$(random_account)
  to=$(random_account)
  # sorteia de novo até dar conta diferente (só 8 contas, poucas tentativas)
  while [[ "$to" == "$from" ]]; do
    to=$(random_account)
  done
  amount=$(random_amount 10 300)
  status=$(auth_post /transfers <<EOF
{
  "fromAccountId": $from,
  "toAccountId": $to,
  "amount": $amount,
  "idempotencyKey": "$(uuid)"
}
EOF
  )
  report "transferência $from -> $to valor=R\$$amount" "$status"
}

echo "==> aporte inicial (garante saldo pra saques/transferências)"
for accountId in "${ACCOUNTS[@]}"; do
  status=$(auth_post /transactions <<EOF
{
  "accountId": $accountId,
  "type": "C",
  "amount": 1000.00,
  "description": "Aporte inicial (seed)",
  "idempotencyKey": "$(uuid)"
}
EOF
  )
  report "aporte    conta=$accountId valor=R\$1000.00" "$status"
done

echo "==> $DEPOSITS depósitos aleatórios"
for ((i = 0; i < DEPOSITS; i++)); do do_deposit; done

echo "==> $WITHDRAWALS saques aleatórios"
for ((i = 0; i < WITHDRAWALS; i++)); do do_withdraw; done

echo "==> $TRANSFERS transferências aleatórias"
for ((i = 0; i < TRANSFERS; i++)); do do_transfer; done

echo
echo "==> feito: $ok chamada(s) ok, $fail falha(s)"
