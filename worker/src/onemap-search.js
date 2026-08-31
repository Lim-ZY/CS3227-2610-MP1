import { fetchWithOneMapToken } from "./onemap-client";
import { getOneMapAccessToken } from "./onemap-token";

const SEARCH_ENDPOINT = "https://www.onemap.gov.sg/api/common/elastic/search";
const MAX_QUERY_LENGTH = 200;

/** Forwards a constrained location lookup to OneMap without exposing its token. */
export async function searchLocations(query, env, {
	fetchFn = fetch,
	getToken = getOneMapAccessToken,
} = {}) {
	if (typeof query !== "string" || query.trim() === "" || query.length > MAX_QUERY_LENGTH) {
		return Response.json({ error: "q must be between 1 and 200 characters." }, { status: 400 });
	}

	try {
		const url = new URL(SEARCH_ENDPOINT);
		url.search = new URLSearchParams({
			searchVal: query.trim(),
			returnGeom: "Y",
			getAddrDetails: "Y",
			pageNum: "1",
		});
		const response = await fetchWithOneMapToken(env,
			token => fetchFn(url, { headers: { Authorization: `Bearer ${token}` } }), { fetchFn, getToken });
		if (!response.ok) {
			console.warn("OneMap location lookup returned a non-success status.", {
				upstreamStatus: response.status,
			});
		}
		return forward(response);
	} catch (error) {
		console.warn("OneMap location lookup failed before a response.", {
			errorName: error instanceof Error ? error.name : typeof error,
		});
		return unavailable();
	}
}

function forward(response) {
	const contentType = response.headers.get("Content-Type");
	return new Response(response.body, {
		status: response.status,
		headers: contentType ? { "Content-Type": contentType } : {},
	});
}

function unavailable() {
	return Response.json({ error: "Live location lookup is temporarily unavailable." }, { status: 503 });
}
