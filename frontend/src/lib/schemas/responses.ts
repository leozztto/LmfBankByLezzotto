import { z } from "zod";

import {
  zAccountStatus,
  zAccountType,
  zAddressType,
  zTxnStatusName,
  zTxnTypeName,
} from "@/lib/enums";

/**
 * Runtime schemas for backend responses. Tolerant (`.passthrough()`) on the
 * display-only shapes; strict only where the UI does math or branches on a value.
 *
 * Jackson serialises `BigDecimal` as a JSON number (`0`, `100.5`); we normalise
 * every money field to a string so downstream code never does float math.
 */

const moneyField = z
  .union([z.string(), z.number()])
  .transform((v) => (typeof v === "number" ? String(v) : v));

export const accountBalanceResponseSchema = z.object({
  availableBalance: moneyField,
  blockedBalance: moneyField,
  totalBalance: moneyField,
});

export const addressResponseSchema = z
  .object({
    id: z.number(),
    zipCode: z.string().nullable(),
    street: z.string().nullable(),
    neighborhood: z.string().nullable(),
    number: z.string().nullable(),
    complement: z.string().nullable(),
    city: z.string().nullable(),
    state: z.string().nullable(),
    country: z.string().nullable(),
    addressType: zAddressType.nullable(),
  })
  .passthrough();

export const accountResponseSchema = z
  .object({
    accountId: z.number(),
    fullName: z.string(),
    maskedDocument: z.string(),
    maskedEmail: z.string(),
    maskedPhone: z.string(),
    accountType: zAccountType,
    accountNumber: z.string(),
    agency: z.string(),
    accountStatus: zAccountStatus,
    createdAt: z.string(),
    updatedAt: z.string().nullable(),
    balance: accountBalanceResponseSchema,
    addresses: z.array(addressResponseSchema),
  })
  .passthrough();

export type AccountResponse = z.infer<typeof accountResponseSchema>;
export type AddressResponse = z.infer<typeof addressResponseSchema>;

export const accountListSchema = z.array(accountResponseSchema);

export const transactionResponseSchema = z
  .object({
    transactionId: z.string(),
    accountId: z.number(),
    type: zTxnTypeName,
    amount: moneyField,
    status: zTxnStatusName,
    description: z.string(),
    createdAt: z.string(),
    transferId: z.string().nullable(),
  })
  .passthrough();

export type TransactionResponse = z.infer<typeof transactionResponseSchema>;

export const bankStatementResponseSchema = z
  .object({
    accountId: z.number(),
    /** all-time ledger balance SUM(CREDIT) - SUM(DEBIT); the number to trust */
    balance: moneyField,
    startDate: z.string().nullable(),
    endDate: z.string().nullable(),
    transactions: z.array(transactionResponseSchema),
  })
  .passthrough();

export type BankStatementResponse = z.infer<typeof bankStatementResponseSchema>;
