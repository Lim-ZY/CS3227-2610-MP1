import { describe, expect, it, vi } from "vitest";
import { searchLocations } from "../src/onemap-search";

describe("OneMap search proxy", () => {
	it("forwards a valid query with a server-side token and fixed OneMap options", async () => {
		const fetchFn = vi.fn().mockResolvedValue(Response.json({ results: [] }));
		const getToken = vi.fn().mockResolvedValue("server-token");

		const response = await searchLocations("COM3", {}, { fetchFn, getToken });

		expect(response.status).toBe(200);
		expect(await response.json()).toEqual({ results: [] });
		expect(getToken).toHaveBeenCalledWith({}, { fetchFn });
		expect(fetchFn).toHaveBeenCalledWith(
			new URL("https://www.onemap.gov.sg/api/common/elastic/search?searchVal=COM3&returnGeom=Y&getAddrDetails=Y&pageNum=1"),
			{ headers: { Authorization: "Bearer server-token" } },
		);
	});

	it.each(["", " ", "x".repeat(201)])("rejects an invalid query", async (query) => {
		const fetchFn = vi.fn();
		const getToken = vi.fn();

		const response = await searchLocations(query, {}, { fetchFn, getToken });

		expect(response.status).toBe(400);
		expect(fetchFn).not.toHaveBeenCalled();
		expect(getToken).not.toHaveBeenCalled();
	});

	it.each([400, 404, 429, 503])("forwards an upstream HTTP %s response", async (status) => {
		const fetchFn = vi.fn().mockResolvedValue(new Response("upstream failure", { status }));
		const getToken = vi.fn().mockResolvedValue("server-token");
		const warning = vi.spyOn(console, "warn").mockImplementation(() => {});

		try {
			const response = await searchLocations("COM3", {}, { fetchFn, getToken });

			expect(response.status).toBe(status);
			expect(await response.text()).toBe("upstream failure");
			expect(warning).toHaveBeenCalledWith("OneMap location lookup returned a non-success status.", {
				upstreamStatus: status,
			});
		} finally {
			warning.mockRestore();
		}
	});

	it.each([401, 403])("refreshes the token once after an HTTP %s response", async (status) => {
		const fetchFn = vi.fn()
			.mockResolvedValueOnce(new Response("expired", { status }))
			.mockResolvedValueOnce(Response.json({ results: [] }));
		const getToken = vi.fn().mockResolvedValueOnce("expired-token").mockResolvedValueOnce("fresh-token");

		const response = await searchLocations("COM3", {}, { fetchFn, getToken });

		expect(response.status).toBe(200);
		expect(getToken).toHaveBeenNthCalledWith(1, {}, { fetchFn });
		expect(getToken).toHaveBeenNthCalledWith(2, {}, { fetchFn, forceRefresh: true });
		expect(fetchFn).toHaveBeenNthCalledWith(1, expect.any(URL), { headers: { Authorization: "Bearer expired-token" } });
		expect(fetchFn).toHaveBeenNthCalledWith(2, expect.any(URL), { headers: { Authorization: "Bearer fresh-token" } });
	});
});
