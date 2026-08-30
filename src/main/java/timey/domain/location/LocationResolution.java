package timey.domain.location;

import java.util.Objects;
import java.util.Optional;

/** The outcome of resolving a location query without exposing provider details. */
public record LocationResolution(Optional<ResolvedLocation> location, String reason,
        boolean isLiveDataServiceUnreachable) {
    /** Performs this operation. */
    public LocationResolution {
        location = Objects.requireNonNull(location);
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Resolution reason must not be blank.");
        }
    }

    public static LocationResolution found(ResolvedLocation location) {
        return new LocationResolution(Optional.of(Objects.requireNonNull(location)), "Location found.", false);
    }

    public static LocationResolution unavailable(String reason) {
        return new LocationResolution(Optional.empty(), reason, false);
    }

    /** Returns an unavailable outcome caused by an unreachable live-data service. */
    public static LocationResolution unreachable(String reason) {
        return new LocationResolution(Optional.empty(), reason, true);
    }

    public boolean isFound() {
        return location.isPresent();
    }
}
