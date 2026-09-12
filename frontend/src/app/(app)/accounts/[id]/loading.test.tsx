import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AccountDetailLoading from "./loading";

describe("AccountDetailLoading", () => {
  it("renders skeletons", () => {
    const { container } = render(<AccountDetailLoading />);
    expect(container.querySelectorAll(".animate-pulse").length).toBe(3);
  });
});
