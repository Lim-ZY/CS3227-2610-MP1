import { fetchWithOneMapToken } from "./onemap-client";
import { getOneMapAccessToken } from "./onemap-token";

const ROUTING_ENDPOINT = "https://www.onemap.gov.sg/api/public/routingsvc/route";
const SINGAPORE_BOUNDS = {
	minLatitude: 1.1,
	maxLatitude: 1.5,
	minLongitude: 103.6,
	maxLongitude: 104.1,
};
const DATE_PATTERN = /^\d{2}-\d{2}-\d{4}$/;
const TIME_PATTERN = /^(?:[01]\d|2[0-3]):[0-5]\d:[0-5]\d$/;

/** Forwards a constrained multi-mode public transport request to OneMap. */
export function findTransitRoutes(request, env, dependencies) {
	return findPublicTransportRoutes(request, "transit", "public transport", env, dependencies);
}

/** Forwards a constrained rail-only public transport request to OneMap. */
export function findRailRoutes(request, env, dependencies) {
	return findPublicTransportRoutes(request, "rail", "rail", env, dependencies);
}

async function findPublicTransportRoutes({ start, end, date, time }, mode, routeDescription, env, {
	fetchFn = fetch,
	getToken = getOneMapAccessToken,
} = {}) {
	const normalizedStart = normalizeCoordinates(start);
	const normalizedEnd = normalizeCoordinates(end);
	if (!normalizedStart || !normalizedEnd || !isValidDate(date) || !TIME_PATTERN.test(time ?? "")) {
		return Response.json({ error: "start, end, date, or time is invalid." }, { status: 400 });
	}

	try {
		const url = new URL(ROUTING_ENDPOINT);
		url.search = new URLSearchParams({
			start: normalizedStart,
			end: normalizedEnd,
			routeType: "pt",
			mode,
			date,
			time,
			numItineraries: "3",
		});
		const response = await fetchWithOneMapToken(env,
			token => fetchFn(url, { headers: { Authorization: `Bearer ${token}` } }), { fetchFn, getToken });
		if (!response.ok) {
			console.warn(`OneMap ${routeDescription} routing request returned a non-success status.`, {
				upstreamStatus: response.status,
			});
		}
		return forward(response);
	} catch (error) {
		console.warn(`OneMap ${routeDescription} routing request failed before a response.`, {
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

function normalizeCoordinates(value) {
	if (typeof value !== "string") {
		return null;
	}
	const parts = value.split(",").map(part => part.trim());
	if (parts.length !== 2 || parts.some(part => part === "")) {
		return null;
	}
	const [latitude, longitude] = parts.map(Number);
	if (!Number.isFinite(latitude) || !Number.isFinite(longitude)
			|| latitude < SINGAPORE_BOUNDS.minLatitude || latitude > SINGAPORE_BOUNDS.maxLatitude
			|| longitude < SINGAPORE_BOUNDS.minLongitude || longitude > SINGAPORE_BOUNDS.maxLongitude) {
		return null;
	}
	return `${latitude},${longitude}`;
}

function isValidDate(value) {
	if (typeof value !== "string" || !DATE_PATTERN.test(value)) {
		return false;
	}
	const [month, day, year] = value.split("-").map(Number);
	const parsed = new Date(Date.UTC(year, month - 1, day));
	return parsed.getUTCFullYear() === year && parsed.getUTCMonth() === month - 1 && parsed.getUTCDate() === day;
}

function unavailable() {
	return Response.json({ error: "Live public transport routing is temporarily unavailable." }, { status: 503 });
}
