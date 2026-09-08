import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

const notFound = vi.fn();
vi.mock("next/navigation", () => ({ notFound: () => notFound() }));
vi.mock("./account-detail", () => ({
  AccountDetail: ({ id }: { id: number }) => <div>detail:{id}</div>,
}));

import AccountDetailPage from "./page";

describe("AccountDetailPage", () => {
  it.each(["abc", "0", "-1", "1.5"])(
    "calls notFound() for the invalid id %s",
    (id) => {
      notFound.mockClear();
      render(<AccountDetailPage params={{ id }} />);
      expect(notFound).toHaveBeenCalled();
    },
  );

  it("renders AccountDetail for a valid id", () => {
    notFound.mockClear();
    render(<AccountDetailPage params={{ id: "42" }} />);
    expect(notFound).not.toHaveBeenCalled();
    expect(screen.getByText("detail:42")).toBeInTheDocument();
  });
});
