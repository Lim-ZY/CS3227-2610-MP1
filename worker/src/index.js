import { searchLocations } from "./onemap-search";
import { findRailRoutes, findTransitRoutes } from "./onemap-route";

export default {
	async fetch(request, env) {
		const url = new URL(request.url);

		if (request.method === "GET" && url.pathname === "/health") {
			return Response.json({ status: "ok" });
		}
		if (request.method === "GET" && url.pathname === "/v1/search") {
			if (url.searchParams.size !== 1 || url.searchParams.getAll("q").length !== 1) {
				return Response.json({ error: "Only one q parameter is allowed." }, { status: 400 });
			}
			return searchLocations(url.searchParams.get("q"), env);
		}
		if (request.method === "GET" && (url.pathname === "/v1/rail-route" || url.pathname === "/v1/transit-route")) {
			const requiredParameters = ["start", "end", "date", "time"];
			if (url.searchParams.size !== requiredParameters.length
					|| requiredParameters.some(parameter => url.searchParams.getAll(parameter).length !== 1)) {
				return Response.json({ error: "Exactly one start, end, date, and time parameter is required." },
					{ status: 400 });
			}
			const routeRequest = Object.fromEntries(requiredParameters.map(parameter =>
				[parameter, url.searchParams.get(parameter)]));
			return url.pathname === "/v1/transit-route"
				? findTransitRoutes(routeRequest, env)
				: findRailRoutes(routeRequest, env);
		}

		return Response.json({ error: "Not found" }, { status: 404 });
	},
};
