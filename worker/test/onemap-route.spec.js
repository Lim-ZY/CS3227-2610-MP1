import { describe, expect, it, vi } from "vitest";
import { findRailRoutes, findTransitRoutes } from "../src/onemap-route";

const VALID_REQUEST = {
	start: "1.2966,103.7764",
	end: "1.2644,103.8223",
	date: "08-27-2026",
	time: "09:15:00",
};

describe("OneMap rail route proxy", () => {
	it("forwards a valid request with a server-side token and fixed rail options", async () => {
		const fetchFn = vi.fn().mockResolvedValue(Response.json({ plan: { itineraries: [] } }));
		const getToken = vi.fn().mockResolvedValue("server-token");

		const response = await findRailRoutes(VALID_REQUEST, {}, { fetchFn, getToken });

		expect(response.status).toBe(200);
		expect(await response.json()).toEqual({ plan: { itineraries: [] } });
		expect(fetchFn).toHaveBeenCalledWith(new URL(
			"https://www.onemap.gov.sg/api/public/routingsvc/route?start=1.2966%2C103.7764&end=1.2644%2C103.8223&routeType=pt&mode=rail&date=08-27-2026&time=09%3A15%3A00&numItineraries=3",
		), { headers: { Authorization: "Bearer server-token" } });
	});

	it.each([
		{ ...VALID_REQUEST, start: "1.0,103.7764" },
		{ ...VALID_REQUEST, end: "not-a-coordinate" },
		{ ...VALID_REQUEST, date: "02-30-2026" },
		{ ...VALID_REQUEST, time: "25:00:00" },
	])("rejects invalid route input", async (request) => {
		const fetchFn = vi.fn();
		const getToken = vi.fn();

		const response = await findRailRoutes(request, {}, { fetchFn, getToken });

		expect(response.status).toBe(400);
		expect(fetchFn).not.toHaveBeenCalled();
		expect(getToken).not.toHaveBeenCalled();
	});

	it.each([400, 404, 429, 503])("forwards an upstream HTTP %s response", async (status) => {
		const fetchFn = vi.fn().mockResolvedValue(new Response("upstream failure", { status }));
		const getToken = vi.fn().mockResolvedValue("server-token");
		const warning = vi.spyOn(console, "warn").mockImplementation(() => {});

		try {
			const response = await findRailRoutes(VALID_REQUEST, {}, { fetchFn, getToken });

			expect(response.status).toBe(status);
			expect(await response.text()).toBe("upstream failure");
			expect(warning).toHaveBeenCalledWith("OneMap rail routing request returned a non-success status.", {
				upstreamStatus: status,
			});
		} finally {
			warning.mockRestore();
		}
	});

	it.each([401, 403])("refreshes the token once after an HTTP %s response", async (status) => {
		const fetchFn = vi.fn()
			.mockResolvedValueOnce(new Response("expired", { status }))
			.mockResolvedValueOnce(Response.json({ plan: { itineraries: [] } }));
		const getToken = vi.fn().mockResolvedValueOnce("expired-token").mockResolvedValueOnce("fresh-token");

		const response = await findRailRoutes(VALID_REQUEST, {}, { fetchFn, getToken });

		expect(response.status).toBe(200);
		expect(getToken).toHaveBeenNthCalledWith(1, {}, { fetchFn });
		expect(getToken).toHaveBeenNthCalledWith(2, {}, { fetchFn, forceRefresh: true });
		expect(fetchFn).toHaveBeenNthCalledWith(1, expect.any(URL), { headers: { Authorization: "Bearer expired-token" } });
		expect(fetchFn).toHaveBeenNthCalledWith(2, expect.any(URL), { headers: { Authorization: "Bearer fresh-token" } });
	});
});

describe("OneMap multi-mode public transport proxy", () => {
	it("forwards a valid request with the TRANSIT mode", async () => {
		const fetchFn = vi.fn().mockResolvedValue(Response.json({ plan: { itineraries: [] } }));
		const getToken = vi.fn().mockResolvedValue("server-token");

		const response = await findTransitRoutes(VALID_REQUEST, {}, { fetchFn, getToken });

		expect(response.status).toBe(200);
		expect(await response.json()).toEqual({ plan: { itineraries: [] } });
		expect(fetchFn).toHaveBeenCalledWith(new URL(
			"https://www.onemap.gov.sg/api/public/routingsvc/route?start=1.2966%2C103.7764&end=1.2644%2C103.8223&routeType=pt&mode=transit&date=08-27-2026&time=09%3A15%3A00&numItineraries=3",
		), { headers: { Authorization: "Bearer server-token" } });
	});
});
