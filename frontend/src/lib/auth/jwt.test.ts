import { describe, expect, it } from "vitest";

import { decodeJwt, isExpired, jwtExpiresAt } from "./jwt";

function makeJwt(payload: Record<string, unknown>) {
  const b64 = (o: unknown) =>
    Buffer.from(JSON.stringify(o)).toString("base64url");
  return `${b64({ alg: "HS256" })}.${b64(payload)}.sig`;
}

describe("decodeJwt", () => {
  it("reads sub and exp from the payload", () => {
    const token = makeJwt({ sub: "alice", exp: 1893456000 });
    expect(decodeJwt(token)).toMatchObject({ sub: "alice", exp: 1893456000 });
  });

  it("returns null for a non-JWT string", () => {
    expect(decodeJwt("not-a-jwt")).toBeNull();
    expect(decodeJwt("a.b")).toBeNull();
  });

  it("returns null when the payload is not valid JSON", () => {
    expect(decodeJwt("header.%%%.sig")).toBeNull();
  });
});

describe("jwtExpiresAt / isExpired", () => {
  it("converts exp seconds to ms", () => {
    const token = makeJwt({ exp: 1000 });
    expect(jwtExpiresAt(token)).toBe(1_000_000);
  });

  it("null when there is no exp", () => {
    expect(jwtExpiresAt(makeJwt({ sub: "x" }))).toBeNull();
  });

  it("detects an expired token", () => {
    const past = makeJwt({ exp: Math.floor(Date.now() / 1000) - 10 });
    const future = makeJwt({ exp: Math.floor(Date.now() / 1000) + 3600 });
    expect(isExpired(past)).toBe(true);
    expect(isExpired(future)).toBe(false);
  });

  it("a token without exp is never expired", () => {
    expect(isExpired(makeJwt({ sub: "x" }))).toBe(false);
  });
});
