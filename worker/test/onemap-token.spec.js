import { describe, expect, it, vi } from "vitest";
import { getOneMapAccessToken, OneMapTokenError } from "../src/onemap-token";

const NOW = 1_700_000_000_000;

function environment({ cachedToken = null, email = "timey@example.com", password = "secret" } = {}) {
	return {
		ONEMAP_EMAIL: email,
		ONEMAP_PASSWORD: password,
		ONEMAP_STATE: {
			get: vi.fn().mockResolvedValue(cachedToken),
			put: vi.fn().mockResolvedValue(undefined),
		},
	};
}

describe("OneMap token provider", () => {
	it("reuses a cached token that is safely before expiry", async () => {
		const env = environment({
			cachedToken: { accessToken: "cached-token", expiresAtMilliseconds: NOW + 600_000 },
		});
		const fetchFn = vi.fn();

		await expect(getOneMapAccessToken(env, { fetchFn, now: () => NOW })).resolves.toBe("cached-token");
		expect(fetchFn).not.toHaveBeenCalled();
		expect(env.ONEMAP_STATE.put).not.toHaveBeenCalled();
	});

	it("refreshes and stores a missing token using Worker secrets", async () => {
		const env = environment();
		const expiryTimestamp = Math.floor((NOW + 3_600_000) / 1000);
		const fetchFn = vi.fn().mockResolvedValue(Response.json({
			access_token: "fresh-token",
			expiry_timestamp: String(expiryTimestamp),
		}));

		await expect(getOneMapAccessToken(env, { fetchFn, now: () => NOW })).resolves.toBe("fresh-token");
		expect(fetchFn).toHaveBeenCalledWith("https://www.onemap.gov.sg/api/auth/post/getToken", {
			method: "POST",
			headers: { "Content-Type": "application/json" },
			body: JSON.stringify({ email: "timey@example.com", password: "secret" }),
		});
		expect(env.ONEMAP_STATE.put).toHaveBeenCalledWith("onemap-access-token", JSON.stringify({
			accessToken: "fresh-token",
			expiresAtMilliseconds: expiryTimestamp * 1000,
		}), { expiration: expiryTimestamp });
	});

	it("forces a refresh without reading a cached token", async () => {
		const env = environment({
			cachedToken: { accessToken: "cached-token", expiresAtMilliseconds: NOW + 600_000 },
		});
		const expiryTimestamp = Math.floor((NOW + 3_600_000) / 1000);
		const fetchFn = vi.fn().mockResolvedValue(Response.json({
			access_token: "fresh-token",
			expiry_timestamp: String(expiryTimestamp),
		}));

		await expect(getOneMapAccessToken(env, { fetchFn, now: () => NOW, forceRefresh: true }))
			.resolves.toBe("fresh-token");
		expect(env.ONEMAP_STATE.get).not.toHaveBeenCalled();
		expect(env.ONEMAP_STATE.put).toHaveBeenCalledOnce();
	});

	it("does not authenticate when required secrets are absent", async () => {
		const env = environment({ email: "", password: "" });
		const fetchFn = vi.fn();

		await expect(getOneMapAccessToken(env, { fetchFn, now: () => NOW }))
			.rejects.toThrow(new OneMapTokenError("ONEMAP_EMAIL is not configured."));
		expect(fetchFn).not.toHaveBeenCalled();
	});

	it("rejects an invalid OneMap authentication response without caching it", async () => {
		const env = environment();
		const fetchFn = vi.fn().mockResolvedValue(Response.json({ access_token: "fresh-token" }));

		await expect(getOneMapAccessToken(env, { fetchFn, now: () => NOW }))
			.rejects.toThrow(new OneMapTokenError("OneMap authentication returned an unusable token."));
		expect(env.ONEMAP_STATE.put).not.toHaveBeenCalled();
	});
});
