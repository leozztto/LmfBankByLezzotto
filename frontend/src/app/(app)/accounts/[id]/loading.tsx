import { Skeleton } from "@/components/ui/skeleton";

export default function AccountDetailLoading() {
  return (
    <div className="max-w-2xl space-y-6">
      <Skeleton className="h-9 w-56" />
      <Skeleton className="h-32 w-full" />
      <Skeleton className="h-48 w-full" />
    </div>
  );
}
