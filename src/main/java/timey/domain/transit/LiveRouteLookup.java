package timey.domain.transit;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Represents either the live routes returned by a provider or the reason the lookup was unavailable. */
public record LiveRouteLookup(List<RouteAlternative> routes, Optional<String> unavailableReason,
        boolean isLiveDataServiceUnreachable, OptionalInt responseStatusCode) {
    /**
     * Creates a successful lookup result, including a valid result with no matching routes.
     *
     * @param routes the routes returned by the provider
     */
    public LiveRouteLookup {
        routes = List.copyOf(routes);
        unavailableReason = unavailableReason.map(String::trim).filter(reason -> !reason.isEmpty());
        responseStatusCode = Objects.requireNonNull(responseStatusCode);
        if (responseStatusCode.isPresent() && (responseStatusCode.getAsInt() < 100
                || responseStatusCode.getAsInt() > 599)) {
            throw new IllegalArgumentException("Response status code must be a valid HTTP status.");
        }
        if (unavailableReason.isEmpty() && (isLiveDataServiceUnreachable || responseStatusCode.isPresent())) {
            throw new IllegalArgumentException("An available lookup cannot include a routing failure.");
        }
        if (isLiveDataServiceUnreachable && responseStatusCode.isPresent()) {
            throw new IllegalArgumentException("An unreachable service cannot include an HTTP response.");
        }
    }

    /**
     * Returns a successful lookup result.
     *
     * @param routes the routes returned by the provider
     * @return a successful result
     */
    public static LiveRouteLookup available(List<RouteAlternative> routes) {
        return new LiveRouteLookup(routes, Optional.empty(), false, OptionalInt.empty());
    }

    /**
     * Returns an unavailable lookup result with a user-facing reason.
     *
     * @param reason the reason live routes could not be retrieved
     * @return an unavailable result
     */
    public static LiveRouteLookup unavailable(String reason) {
        return new LiveRouteLookup(List.of(), Optional.of(reason), false, OptionalInt.empty());
    }

    /** Returns an unavailable lookup caused by a received live-data response. */
    public static LiveRouteLookup unavailable(int responseStatusCode, String reason) {
        return new LiveRouteLookup(List.of(), Optional.of(reason), false, OptionalInt.of(responseStatusCode));
    }

    /** Returns an unavailable lookup caused by an unreachable live-data service. */
    public static LiveRouteLookup unreachable(String reason) {
        return new LiveRouteLookup(List.of(), Optional.of(reason), true, OptionalInt.empty());
    }

    /**
     * Returns whether the provider completed the lookup successfully.
     *
     * @return true when the lookup completed successfully
     */
    public boolean isAvailable() {
        return unavailableReason.isEmpty();
    }
}
