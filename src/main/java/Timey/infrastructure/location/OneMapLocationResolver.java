package Timey.infrastructure.location;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Timey.domain.location.LocationResolution;
import Timey.domain.location.ResolvedLocation;
import Timey.infrastructure.http.HttpRequester;
import Timey.ports.LocationResolver;

/** Resolves Singapore addresses through Timey's server-held live-data service. */
public final class OneMapLocationResolver implements LocationResolver {
    private static final Pattern ADDRESS = Pattern.compile("\\\"ADDRESS\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern SEARCH_VALUE = Pattern.compile("\\\"SEARCHVAL\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern LATITUDE = Pattern.compile("\\\"LATITUDE\\\"\\s*:\\s*\\\"?([-+0-9.]+)\\\"?");
    private static final Pattern LONGITUDE = Pattern.compile("\\\"LONGITUDE\\\"\\s*:\\s*\\\"?([-+0-9.]+)\\\"?");

    private final HttpRequester httpRequester;
    private final Optional<URI> liveDataBaseUri;

    public OneMapLocationResolver(HttpRequester httpRequester, Optional<URI> liveDataBaseUri) {
        this.httpRequester = httpRequester;
        this.liveDataBaseUri = liveDataBaseUri;
    }

    @Override
    public LocationResolution resolve(String query) {
        if (query == null || query.isBlank()) {
            return LocationResolution.unavailable("A location is required for online lookup.");
        }
        if (liveDataBaseUri.isEmpty()) {
            return LocationResolution.unavailable("Live location lookup is not configured.");
        }
        try {
            var response = httpRequester.get(searchUri(query));
            if (response.statusCode() != 200) {
                return LocationResolution.unavailable("OneMap lookup is temporarily unavailable (HTTP "
                        + response.statusCode() + ").");
            }
            return parseFirstResult(response.body()).map(LocationResolution::found)
                    .orElseGet(() -> LocationResolution.unavailable("OneMap could not find \"" + query + "\"."));
        } catch (IllegalStateException exception) {
            return LocationResolution.unavailable("OneMap lookup is temporarily unavailable.");
        }
    }

    private URI searchUri(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return URI.create(liveDataBaseUri.orElseThrow().toString().replaceAll("/$", "")
                + "/v1/search?q=" + encodedQuery);
    }

    private Optional<ResolvedLocation> parseFirstResult(String body) {
        return first(SEARCH_VALUE, body).flatMap(displayName -> first(ADDRESS, body).flatMap(address ->
                first(LATITUDE, body).flatMap(latitude -> first(LONGITUDE, body).flatMap(longitude -> {
                    try {
                        return Optional.of(new ResolvedLocation(displayName, address,
                                Double.parseDouble(latitude), Double.parseDouble(longitude)));
                    } catch (IllegalArgumentException exception) {
                        return Optional.empty();
                    }
                }))));
    }

    private Optional<String> first(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
