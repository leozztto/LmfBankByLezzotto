import { describe, expect, it, vi } from "vitest";

vi.mock("sonner", () => ({
  toast: Object.assign(vi.fn(), { success: vi.fn(), error: vi.fn() }),
}));

import { toast } from "sonner";
import { notify } from "./notify";

describe("notify", () => {
  it("delegates to sonner", () => {
    notify.success("ok");
    notify.error("bad");
    notify.info("fyi");
    expect(toast.success).toHaveBeenCalledWith("ok");
    expect(toast.error).toHaveBeenCalledWith("bad");
    expect(toast).toHaveBeenCalledWith("fyi");
  });
});
