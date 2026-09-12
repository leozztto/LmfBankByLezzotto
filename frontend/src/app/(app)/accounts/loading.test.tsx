import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AccountsLoading from "./loading";

describe("AccountsLoading", () => {
  it("renders skeletons", () => {
    const { container } = render(<AccountsLoading />);
    expect(container.querySelectorAll(".animate-pulse").length).toBe(2);
  });
});
