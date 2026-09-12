import { toast } from "sonner";

/**
 * Thin wrapper over sonner so tests mock a single module and the app has one
 * place to tweak toast behaviour.
 */
export const notify = {
  success: (message: string) => toast.success(message),
  error: (message: string) => toast.error(message),
  info: (message: string) => toast(message),
};
