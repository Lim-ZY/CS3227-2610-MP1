import { describe, expect, it, vi } from "vitest";
import { fetchWithOneMapToken } from "../src/onemap-client";

describe("OneMap authenticated request", () => {
	it("forwards a non-authentication response without retrying", async () => {
		const getToken = vi.fn().mockResolvedValue("server-token");
		const request = vi.fn().mockResolvedValue(new Response("busy", { status: 429 }));

		const response = await fetchWithOneMapToken({}, request, { fetchFn: vi.fn(), getToken });

		expect(response.status).toBe(429);
		expect(request).toHaveBeenCalledOnce();
		expect(getToken).toHaveBeenCalledOnce();
	});

	it.each([401, 403])("refreshes once after an HTTP %s response", async (status) => {
		const getToken = vi.fn().mockResolvedValueOnce("expired-token").mockResolvedValueOnce("fresh-token");
		const request = vi.fn()
			.mockResolvedValueOnce(new Response("expired", { status }))
			.mockResolvedValueOnce(new Response("still expired", { status }));

		const response = await fetchWithOneMapToken({}, request, { fetchFn: vi.fn(), getToken });

		expect(response.status).toBe(status);
		expect(request).toHaveBeenNthCalledWith(1, "expired-token");
		expect(request).toHaveBeenNthCalledWith(2, "fresh-token");
		expect(getToken).toHaveBeenCalledTimes(2);
	});
});
