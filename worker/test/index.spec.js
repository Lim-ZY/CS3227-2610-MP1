import {
	env,
	createExecutionContext,
	waitOnExecutionContext,
	SELF,
} from "cloudflare:test";
import { describe, it, expect } from "vitest";
import worker from "../src";

describe("Timey live-data worker", () => {
	it("reports healthy through the public health endpoint", async () => {
		const request = new Request("http://example.com/health");
		const ctx = createExecutionContext();
		const response = await worker.fetch(request, env, ctx);
		await waitOnExecutionContext(ctx);

		expect(response.status).toBe(200);
		expect(await response.json()).toEqual({ status: "ok" });
	});

	it("rejects unknown paths", async () => {
		const response = await SELF.fetch("http://example.com/unknown");

		expect(response.status).toBe(404);
		expect(await response.json()).toEqual({ error: "Not found" });
	});

	it("rejects unsupported methods on the health endpoint", async () => {
		const response = await SELF.fetch("http://example.com/health", { method: "POST" });

		expect(response.status).toBe(404);
		expect(await response.json()).toEqual({ error: "Not found" });
	});

	it("rejects search requests with unexpected query parameters", async () => {
		const response = await SELF.fetch("http://example.com/v1/search?q=COM3&page=2");

		expect(response.status).toBe(400);
		expect(await response.json()).toEqual({ error: "Only one q parameter is allowed." });
	});

	it("rejects transit route requests with unexpected query parameters", async () => {
		const response = await SELF.fetch("http://example.com/v1/transit-route?start=1.2966,103.7764&end=1.2644,103.8223&date=08-27-2026&time=09:15:00&mode=drive");

		expect(response.status).toBe(400);
		expect(await response.json()).toEqual({
			error: "Exactly one start, end, date, and time parameter is required.",
		});
	});
});
