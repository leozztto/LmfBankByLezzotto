import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import DashboardLoading from "./loading";

describe("DashboardLoading", () => {
  it("renders skeletons", () => {
    const { container } = render(<DashboardLoading />);
    expect(container.querySelectorAll(".animate-pulse").length).toBe(2);
  });
});
