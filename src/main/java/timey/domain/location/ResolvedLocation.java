package timey.domain.location;

/** A Singapore location returned by a location provider. */
public record ResolvedLocation(String displayName, String address, double latitude, double longitude) {
    /** Performs this operation. */
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
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees.");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees.");
        }
    }
}
