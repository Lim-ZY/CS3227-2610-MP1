const TOKEN_KEY = "onemap-access-token";
const REFRESH_BUFFER_MILLISECONDS = 5 * 60 * 1000;
const TOKEN_ENDPOINT = "https://www.onemap.gov.sg/api/auth/post/getToken";

export class OneMapTokenError extends Error {}

/** Retrieves a reusable OneMap token without exposing credentials to clients. */
export async function getOneMapAccessToken(env, { fetchFn = fetch, now = Date.now, forceRefresh = false } = {}) {
	const cachedToken = forceRefresh ? null : await env.ONEMAP_STATE.get(TOKEN_KEY, "json");
	if (isUsable(cachedToken, now())) {
		return cachedToken.accessToken;
	}

	return refreshToken(env, fetchFn, now());
}

async function refreshToken(env, fetchFn, now) {
	const email = requiredSecret(env.ONEMAP_EMAIL, "ONEMAP_EMAIL");
	const password = requiredSecret(env.ONEMAP_PASSWORD, "ONEMAP_PASSWORD");
	const response = await fetchFn(TOKEN_ENDPOINT, {
		method: "POST",
		headers: { "Content-Type": "application/json" },
		body: JSON.stringify({ email, password }),
	});

	if (!response.ok) {
		throw new OneMapTokenError("OneMap authentication is temporarily unavailable.");
	}

	let body;
	try {
		body = await response.json();
	} catch {
		throw new OneMapTokenError("OneMap authentication returned an invalid response.");
	}

	const token = body.access_token;
	const expiresAtMilliseconds = Number(body.expiry_timestamp) * 1000;
	if (typeof token !== "string" || token.trim() === "" || !Number.isFinite(expiresAtMilliseconds)
			|| expiresAtMilliseconds <= now + REFRESH_BUFFER_MILLISECONDS) {
		throw new OneMapTokenError("OneMap authentication returned an unusable token.");
	}

	const cachedToken = { accessToken: token, expiresAtMilliseconds };
	await env.ONEMAP_STATE.put(TOKEN_KEY, JSON.stringify(cachedToken), {
		expiration: Math.floor(expiresAtMilliseconds / 1000),
	});
	return token;
}

function isUsable(token, now) {
	return token && typeof token.accessToken === "string" && token.accessToken.trim() !== ""
		&& Number.isFinite(token.expiresAtMilliseconds)
		&& token.expiresAtMilliseconds > now + REFRESH_BUFFER_MILLISECONDS;
}

function requiredSecret(value, name) {
	if (typeof value !== "string" || value.trim() === "") {
		throw new OneMapTokenError(`${name} is not configured.`);
	}
	return value;
}
