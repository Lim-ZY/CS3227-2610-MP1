package Timey.domain.location;

/** A Singapore location returned by a location provider. */
public record ResolvedLocation(String displayName, String address, double latitude, double longitude) {
    public ResolvedLocation {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank.");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Address must not be blank.");
        }
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)) {
            throw new IllegalArgumentException("Coordinates must be finite numbers.");
        }
    }
}
