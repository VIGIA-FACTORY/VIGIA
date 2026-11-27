import { describe, expect, it } from "vitest";
import { toApiError } from "./api";
import { readConfig } from "./config";

describe("readConfig", () => {
  it("trims trailing slashes and defaults the client", () => {
    expect(readConfig({ authority: "https://auth.x/realms/vigia/", apiUrl: "https://api.x/" })).toEqual({
      authority: "https://auth.x/realms/vigia",
      apiUrl: "https://api.x",
      clientId: "vigia-admin",
    });
  });

  it("fails without the authority", () => {
    expect(() => readConfig({ apiUrl: "https://api.x" })).toThrow("Missing authority");
  });
});

describe("toApiError", () => {
  it("reads the code and detail of a problem detail", async () => {
    const response = new Response(JSON.stringify({ code: "DUPLICATE_PLATE", detail: "Plate taken" }), {
      status: 409,
    });
    const error = await toApiError(response);
    expect([error.status, error.code, error.message]).toEqual([409, "DUPLICATE_PLATE", "Plate taken"]);
  });
});
