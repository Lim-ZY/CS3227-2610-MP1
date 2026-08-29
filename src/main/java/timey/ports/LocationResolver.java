package timey.ports;

import timey.domain.location.LocationResolution;

/** Resolves a user-entered Singapore location into coordinates. */
public interface LocationResolver {
    LocationResolution resolve(String query);
}
