import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import AppLoading from "./loading";

describe("AppLoading", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<AppLoading />);
    expect(container.querySelectorAll(".animate-pulse").length).toBe(3);
  });
});
