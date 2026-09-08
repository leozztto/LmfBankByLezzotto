import { notFound } from "next/navigation";

import { AccountDetail } from "./account-detail";

export default function AccountDetailPage({
  params,
}: {
  params: { id: string };
}) {
  const id = Number(params.id);
  if (!Number.isInteger(id) || id <= 0) {
    notFound();
  }
  return <AccountDetail id={id} />;
}
