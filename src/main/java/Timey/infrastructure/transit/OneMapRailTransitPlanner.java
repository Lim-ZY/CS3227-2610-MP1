package Timey.infrastructure.transit;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.RouteAlternative;
import Timey.infrastructure.http.HttpRequester;
import Timey.ports.RailTransitPlanner;

/** OneMap public-transport adapter limited to rail itineraries. */
public final class OneMapRailTransitPlanner implements RailTransitPlanner {
    private static final String ROUTING_ENDPOINT = "https://www.onemap.gov.sg/api/public/routingsvc/route";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-uuuu");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern DURATION = Pattern.compile("\\\"duration\\\"\\s*:\\s*(\\d+)");
    private static final Pattern WALK_TIME = Pattern.compile("\\\"walkTime\\\"\\s*:\\s*(\\d+)");
    private static final Pattern TRANSIT_TIME = Pattern.compile("\\\"transitTime\\\"\\s*:\\s*(\\d+)");
    private static final Pattern TRANSFERS = Pattern.compile("\\\"transfers\\\"\\s*:\\s*(\\d+)");

    private final HttpRequester httpRequester;
    private final Optional<String> accessToken;

    public OneMapRailTransitPlanner(HttpRequester httpRequester, Optional<String> accessToken) {
        this.httpRequester = httpRequester;
        this.accessToken = accessToken.filter(token -> !token.isBlank());
    }

    @Override
    public List<RouteAlternative> findRoutes(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime) {
        if (accessToken.isEmpty()) {
            return List.of();
        }
        try {
            var response = httpRequester.get(routeUri(origin, destination, departureDate, departureTime),
                    accessToken.orElseThrow());
            return response.statusCode() == 200 ? parseItineraries(response.body()) : List.of();
        } catch (IllegalStateException exception) {
            return List.of();
        }
    }

    private URI routeUri(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime) {
        String start = origin.latitude() + "," + origin.longitude();
        String end = destination.latitude() + "," + destination.longitude();
        return URI.create(ROUTING_ENDPOINT + "?start=" + start + "&end=" + end
                + "&routeType=pt&mode=rail&date=" + DATE_FORMAT.format(departureDate)
                + "&time=" + TIME_FORMAT.format(departureTime) + "&numItineraries=3");
    }

    private List<RouteAlternative> parseItineraries(String body) {
        List<RouteAlternative> routes = new ArrayList<>();
        for (String itinerary : itineraryObjects(body)) {
            Optional<Long> duration = number(DURATION, itinerary);
            Optional<Long> walkTime = number(WALK_TIME, itinerary);
            Optional<Long> transitTime = number(TRANSIT_TIME, itinerary);
            Optional<Long> transfers = number(TRANSFERS, itinerary);
            if (duration.isPresent() && walkTime.isPresent() && transitTime.isPresent() && transfers.isPresent()) {
                routes.add(new RouteAlternative("Live rail route " + (routes.size() + 1),
                        Duration.ofSeconds(walkTime.orElseThrow()), Duration.ofSeconds(transitTime.orElseThrow()),
                        Math.toIntExact(transfers.orElseThrow())));
            }
        }
        return routes;
    }

    private List<String> itineraryObjects(String body) {
        int arrayStart = body.indexOf("\"itineraries\"");
        if (arrayStart < 0) {
            return List.of();
        }
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int objectStart = -1;
        for (int index = body.indexOf('[', arrayStart); index >= 0 && index < body.length(); index++) {
            char character = body.charAt(index);
            if (character == '{') {
                if (depth++ == 0) {
                    objectStart = index;
                }
            } else if (character == '}' && --depth == 0 && objectStart >= 0) {
                objects.add(body.substring(objectStart, index + 1));
                objectStart = -1;
            } else if (character == ']' && depth == 0) {
                return objects;
            }
        }
        return List.of();
    }

    private Optional<Long> number(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(Long.parseLong(matcher.group(1))) : Optional.empty();
    }
}
