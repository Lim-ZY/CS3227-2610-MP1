package timey.domain.transit;

import java.util.List;
import java.util.Optional;

/** Represents either the live routes returned by a provider or the reason the lookup was unavailable. */
public record LiveRouteLookup(List<RouteAlternative> routes, Optional<String> unavailableReason) {
    /**
     * Creates a successful lookup result, including a valid result with no matching routes.
     *
     * @param routes the routes returned by the provider
     */
    public LiveRouteLookup {
        routes = List.copyOf(routes);
        unavailableReason = unavailableReason.map(String::trim).filter(reason -> !reason.isEmpty());
    }

    /**
     * Returns a successful lookup result.
     *
     * @param routes the routes returned by the provider
     * @return a successful result
     */
    public static LiveRouteLookup available(List<RouteAlternative> routes) {
        return new LiveRouteLookup(routes, Optional.empty());
    }

    /**
     * Returns an unavailable lookup result with a user-facing reason.
     *
     * @param reason the reason live routes could not be retrieved
     * @return an unavailable result
     */
    public static LiveRouteLookup unavailable(String reason) {
        return new LiveRouteLookup(List.of(), Optional.of(reason));
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
