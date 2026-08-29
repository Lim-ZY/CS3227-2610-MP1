package timey.infrastructure.transit;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import timey.domain.location.ResolvedLocation;
import timey.domain.transit.LiveRouteLookup;
import timey.domain.transit.RouteAlternative;
import timey.domain.transit.RouteStep;
import timey.domain.transit.RouteStepMode;
import timey.infrastructure.http.HttpRequester;
import timey.ports.RailTransitPlanner;

/** Live-data adapter for server-authenticated OneMap rail itineraries. */
public final class OneMapRailTransitPlanner implements RailTransitPlanner {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-uuuu");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpRequester httpRequester;
    private final Optional<URI> liveDataBaseUri;

    /** Creates a new OneMapRailTransitPlanner. */
    public OneMapRailTransitPlanner(HttpRequester httpRequester, Optional<URI> liveDataBaseUri) {
        this.httpRequester = httpRequester;
        this.liveDataBaseUri = liveDataBaseUri;
    }

    @Override
    public LiveRouteLookup findRoutes(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime) {
        if (liveDataBaseUri.isEmpty()) {
            return LiveRouteLookup.unavailable("Live rail routing is not configured.");
        }
        try {
            var response = httpRequester.get(routeUri(origin, destination, departureDate, departureTime));
            if (response.statusCode() != 200) {
                return LiveRouteLookup.unavailable("OneMap routing failed (HTTP " + response.statusCode() + ").");
            }
            return parseItineraries(response.body());
        } catch (IllegalStateException exception) {
            return LiveRouteLookup.unavailable("OneMap routing timed out or is temporarily unavailable.");
        }
    }

    private URI routeUri(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime) {
        String start = origin.latitude() + "," + origin.longitude();
        String end = destination.latitude() + "," + destination.longitude();
        return URI.create(liveDataBaseUri.orElseThrow().toString().replaceAll("/$", "")
                + "/v1/rail-route?start=" + start + "&end=" + end
                + "&date=" + DATE_FORMAT.format(departureDate) + "&time=" + TIME_FORMAT.format(departureTime));
    }

    /** Parses every itinerary in OneMap's response instead of relying on partial text matching. */
    private LiveRouteLookup parseItineraries(String body) {
        try {
            JsonNode itineraries = OBJECT_MAPPER.readTree(body).path("plan").path("itineraries");
            if (!itineraries.isArray()) {
                return LiveRouteLookup.unavailable("OneMap routing returned an invalid response.");
            }

            List<RouteAlternative> routes = new ArrayList<>();
            for (JsonNode itinerary : itineraries) {
                Optional<RouteAlternative> route = routeAlternative(itinerary, routes.size() + 1);
                if (route.isEmpty()) {
                    return LiveRouteLookup.unavailable("OneMap routing returned an incomplete itinerary.");
                }
                routes.add(route.orElseThrow());
            }
            return LiveRouteLookup.available(routes);
        } catch (JsonProcessingException exception) {
            return LiveRouteLookup.unavailable("OneMap routing returned an unreadable response.");
        }
    }

    /** Maps a complete OneMap itinerary to the route model used by the command-line application. */
    private Optional<RouteAlternative> routeAlternative(JsonNode itinerary, int routeNumber) {
        Optional<Long> walkTime = number(itinerary, "walkTime");
        Optional<Long> transitTime = number(itinerary, "transitTime");
        Optional<Long> transfers = number(itinerary, "transfers");
        if (walkTime.isEmpty() || transitTime.isEmpty() || transfers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new RouteAlternative("Live rail route " + routeNumber,
                Duration.ofSeconds(walkTime.orElseThrow()), Duration.ofSeconds(transitTime.orElseThrow()),
                Math.toIntExact(transfers.orElseThrow()), routeSteps(itinerary.path("legs"))));
    }

    /** Extracts displayable walk and rail legs while tolerating missing optional leg data. */
    private List<RouteStep> routeSteps(JsonNode legs) {
        if (!legs.isArray()) {
            return List.of();
        }
        List<RouteStep> steps = new ArrayList<>();
        for (JsonNode leg : legs) {
            routeStep(leg).ifPresent(steps::add);
        }
        return steps;
    }

    private Optional<RouteStep> routeStep(JsonNode leg) {
        Optional<Long> duration = number(leg, "duration");
        String from = leg.path("from").path("name").asText("");
        String to = leg.path("to").path("name").asText("");
        String mode = leg.path("mode").asText("");
        if (duration.isEmpty() || from.isBlank() || to.isBlank()) {
            return Optional.empty();
        }
        if ("WALK".equalsIgnoreCase(mode)) {
            return Optional.of(new RouteStep(RouteStepMode.WALK, from, to, "walking",
                    Duration.ofSeconds(duration.orElseThrow())));
        }
        String service = firstNonBlank(leg.path("routeShortName").asText(""), leg.path("route").asText(""),
                leg.path("routeLongName").asText(""));
        return service.isBlank() ? Optional.empty()
                : Optional.of(new RouteStep(RouteStepMode.RAIL, from, to, service,
                        Duration.ofSeconds(duration.orElseThrow())));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    /** Returns an integral itinerary field, if it is present and representable as a long. */
    private Optional<Long> number(JsonNode itinerary, String fieldName) {
        JsonNode value = itinerary.get(fieldName);
        return value != null && value.canConvertToLong() ? Optional.of(value.longValue()) : Optional.empty();
    }
}
