import { Skeleton } from "@/components/ui/skeleton";

/**
 * Generic segment fallback for `(app)` routes without their own `loading.tsx`
 * (open-account, deposit-withdraw, transfer, statement).
 */
export default function AppLoading() {
  return (
    <div className="mx-auto max-w-lg space-y-4 py-2">
      <Skeleton className="h-8 w-48" />
      <Skeleton className="h-4 w-64" />
      <Skeleton className="h-64 w-full" />
    </div>
  );
}
