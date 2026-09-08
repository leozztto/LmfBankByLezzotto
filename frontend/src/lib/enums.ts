import { z } from "zod";

/**
 * Wire formats for the backend enums (verified against the Java sources):
 *
 * - account-domain enums serialize AND deserialize as the short CODE
 *   (`"BR"`, `"C"`, `"A"`, `"R"`);
 * - movement-domain enums deserialize from a code but serialize as the NAME
 *   (`"CREDIT"`, `"COMPLETED"`) — asymmetric, so request vs response schemas differ.
 *
 * Labels here are PT-BR and deliberately override the inconsistent backend
 * `description` fields (some English, some Portuguese).
 */

export const zNationality = z.enum(["BR", "FR"]);
export const zAccountType = z.enum(["C", "S"]);
export const zAccountStatus = z.enum(["A", "B", "C"]);
export const zAddressType = z.enum(["R", "C", "B"]);
/** request body of POST /transactions */
export const zTxnTypeReq = z.enum(["C", "D"]);
/** response body `type` field */
export const zTxnTypeName = z.enum(["CREDIT", "DEBIT"]);
/** response body `status` field (also used by TransferResponse) */
export const zTxnStatusName = z.enum(["PENDING", "COMPLETED", "FAILED"]);

export type NationalityCode = z.infer<typeof zNationality>;
export type AccountTypeCode = z.infer<typeof zAccountType>;
export type AccountStatusCode = z.infer<typeof zAccountStatus>;
export type AddressTypeCode = z.infer<typeof zAddressType>;
export type TxnTypeName = z.infer<typeof zTxnTypeName>;
export type TxnStatusName = z.infer<typeof zTxnStatusName>;

export const nationalityLabels: Record<NationalityCode, string> = {
  BR: "Brasileira",
  FR: "Estrangeira",
};

export const accountTypeLabels: Record<AccountTypeCode, string> = {
  C: "Conta corrente",
  S: "Conta poupança",
};

export const accountStatusLabels: Record<AccountStatusCode, string> = {
  A: "Ativa",
  B: "Bloqueada",
  C: "Encerrada",
};

export const addressTypeLabels: Record<AddressTypeCode, string> = {
  R: "Residencial",
  C: "Comercial",
  B: "Cobrança",
};

/** covers both the request code ("C"/"D") and the response name */
export const txnTypeLabels: Record<string, string> = {
  C: "Crédito",
  D: "Débito",
  CREDIT: "Crédito",
  DEBIT: "Débito",
};

export const txnStatusLabels: Record<TxnStatusName, string> = {
  PENDING: "Pendente",
  COMPLETED: "Concluída",
  FAILED: "Falhou",
};

type Option = { value: string; label: string };

const toOptions = (labels: Record<string, string>): Option[] =>
  Object.entries(labels).map(([value, label]) => ({ value, label }));

export const nationalityOptions = toOptions(nationalityLabels);
export const accountTypeOptions = toOptions(accountTypeLabels);
export const addressTypeOptions = toOptions(addressTypeLabels);
