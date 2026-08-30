package timey.domain.location;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** The outcome of resolving a location query without exposing provider details. */
public record LocationResolution(Optional<ResolvedLocation> location, String reason,
        boolean isLiveDataServiceUnreachable, boolean isNotFound, OptionalInt responseStatusCode) {
    /** Performs this operation. */
    public LocationResolution {
        location = Objects.requireNonNull(location);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Resolution reason must not be blank.");
        }
        responseStatusCode = Objects.requireNonNull(responseStatusCode);
        if (responseStatusCode.isPresent() && (responseStatusCode.getAsInt() < 100
                || responseStatusCode.getAsInt() > 599)) {
            throw new IllegalArgumentException("Response status code must be a valid HTTP status.");
        }
        if (location.isPresent() && (isLiveDataServiceUnreachable || isNotFound || responseStatusCode.isPresent())) {
            throw new IllegalArgumentException("A found location cannot include a lookup failure.");
        }
        if (isLiveDataServiceUnreachable && (isNotFound || responseStatusCode.isPresent())) {
            throw new IllegalArgumentException("An unreachable service cannot include another failure state.");
        }
    }

    /** Returns a successful location resolution. */
    public static LocationResolution found(ResolvedLocation location) {
        return new LocationResolution(Optional.of(Objects.requireNonNull(location)), "Location found.", false, false,
                OptionalInt.empty());
    }

    /** Returns an unavailable outcome without an HTTP response. */
    public static LocationResolution unavailable(String reason) {
        return new LocationResolution(Optional.empty(), reason, false, false, OptionalInt.empty());
    }

    /** Returns an unavailable outcome caused by a received live-data response. */
    public static LocationResolution unavailable(int responseStatusCode, String reason) {
        return new LocationResolution(Optional.empty(), reason, false, false, OptionalInt.of(responseStatusCode));
    }

    /** Returns an unavailable outcome caused by an unmatched location query. */
    public static LocationResolution notFound(String reason) {
        return new LocationResolution(Optional.empty(), reason, false, true, OptionalInt.empty());
    }

    /** Returns an unavailable outcome caused by an unreachable live-data service. */
    public static LocationResolution unreachable(String reason) {
        return new LocationResolution(Optional.empty(), reason, true, false, OptionalInt.empty());
    }

    public boolean isFound() {
        return location.isPresent();
    }
}
