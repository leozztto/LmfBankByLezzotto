import { z } from "zod";

import {
  zAccountStatus,
  zAccountType,
  zAddressType,
} from "@/lib/enums";

/**
 * Runtime schemas for backend responses. Tolerant (`.passthrough()`) on the
 * display-only shapes; strict only where the UI does math or branches on a value.
 */

export const accountBalanceResponseSchema = z.object({
  availableBalance: z.string(),
  blockedBalance: z.string(),
  totalBalance: z.string(),
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
