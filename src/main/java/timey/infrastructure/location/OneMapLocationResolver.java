package timey.infrastructure.location;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import timey.domain.location.LocationResolution;
import timey.domain.location.ResolvedLocation;
import timey.infrastructure.http.HttpRequester;
import timey.infrastructure.http.HttpResult;
import timey.infrastructure.http.WorkerErrorMessageParser;
import timey.ports.LocationResolver;

/** Resolves Singapore addresses through Timey's server-held live-data service. */
public final class OneMapLocationResolver implements LocationResolver {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpRequester httpRequester;
    private final Optional<URI> liveDataBaseUri;

    /** Creates a new OneMapLocationResolver. */
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
        HttpResult response;
        try {
            response = httpRequester.get(searchUri(query));
        } catch (RuntimeException exception) {
            return LocationResolution.unreachable("OneMap lookup is temporarily unavailable.");
        }
        if (response.statusCode() != 200) {
            return LocationResolution.unavailable(response.statusCode(),
                    WorkerErrorMessageParser.extract(response.body(),
                            "OneMap location lookup is temporarily unavailable."));
        }
        return resolveResponse(response.body(), query);
    }

    private LocationResolution resolveResponse(String body, String query) {
        try {
            List<ResolvedLocation> locations = parseLocations(body);
            if (locations.isEmpty()) {
                return LocationResolution.notFound("OneMap could not find \"" + query + "\".");
            }
            return LocationResolution.found(locations.getFirst());
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return LocationResolution.unavailable("OneMap lookup returned an unreadable response.");
        }
    }

    private URI searchUri(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return URI.create(liveDataBaseUri.orElseThrow().toString().replaceAll("/$", "")
                + "/v1/search?q=" + encodedQuery);
    }

    private List<ResolvedLocation> parseLocations(String body) throws JsonProcessingException {
        JsonNode root = OBJECT_MAPPER.readTree(body);
        if (root == null) {
            throw new IllegalArgumentException("OneMap response must not be empty.");
        }
        JsonNode results = root.path("results");
        if (!results.isArray()) {
            throw new IllegalArgumentException("OneMap results must be an array.");
        }
        List<ResolvedLocation> locations = new ArrayList<>();
        for (JsonNode result : results) {
            toLocation(result).ifPresent(locations::add);
        }
        return locations;
    }

    private Optional<ResolvedLocation> toLocation(JsonNode result) {
        try {
            String displayName = requiredText(result, "SEARCHVAL");
            String address = requiredText(result, "ADDRESS");
            double latitude = requiredCoordinate(result, "LATITUDE");
            double longitude = requiredCoordinate(result, "LONGITUDE");
            return Optional.of(new ResolvedLocation(displayName, address, latitude, longitude));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String requiredText(JsonNode result, String fieldName) {
        JsonNode value = result.path(fieldName);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException("OneMap result field is missing or invalid.");
        }
        return value.asText();
    }

    private double requiredCoordinate(JsonNode result, String fieldName) {
        JsonNode value = result.path(fieldName);
        if ((!value.isTextual() && !value.isNumber()) || value.asText().isBlank()) {
            throw new IllegalArgumentException("OneMap result coordinate is missing or invalid.");
        }
        return Double.parseDouble(value.asText());
    }

}
