/**
 * Commitlint para todo o monorepo, a partir da Fase 3.7.
 *
 * O time escreve as mensagens em português e em formato livre, então todas as
 * regras estruturais do padrão Conventional Commits ficam desligadas — inclusive
 * o limite de comprimento do cabeçalho. Na prática, o commitlint não bloqueia
 * nenhuma mensagem; fica só como ponto de extensão futuro.
 */
export default {
  extends: ["@commitlint/config-conventional"],
  rules: {
    // Não exigimos o formato "tipo(escopo): descrição".
    "type-empty": [0],
    "type-enum": [0],
    "type-case": [0],
    "scope-empty": [0],
    "scope-case": [0],
    "subject-empty": [0],
    "subject-case": [0],
    "subject-full-stop": [0],
    // Sem limite de comprimento.
    "header-max-length": [0],
    "body-max-line-length": [0],
  },
};
