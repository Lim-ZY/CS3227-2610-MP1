import { getOneMapAccessToken } from "./onemap-token";

/** Executes one authenticated OneMap request and refreshes an expired access token once. */
export async function fetchWithOneMapToken(env, request, { fetchFn = fetch, getToken = getOneMapAccessToken } = {}) {
	let token = await getToken(env, { fetchFn });
	const response = await request(token);
	if (!isTokenRejected(response.status)) {
		return response;
	}

	token = await getToken(env, { fetchFn, forceRefresh: true });
	return request(token);
}

function isTokenRejected(status) {
	return status === 401 || status === 403;
}
