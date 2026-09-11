export const queryKeys = {
  session: ["session"] as const,
  accounts: {
    all: ["accounts"] as const,
    list: () => ["accounts", "list"] as const,
    detail: (id: number) => ["accounts", "detail", id] as const,
    byDocument: (doc: string) => ["accounts", "by-document", doc] as const,
    byNumber: (accountNumber: string) =>
      ["accounts", "by-number", accountNumber] as const,
  },
  statement: (accountId: number, range?: { start: string; end: string }) =>
    ["statement", accountId, range ?? "all"] as const,
};
