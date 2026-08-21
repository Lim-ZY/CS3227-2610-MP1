package Timey.ports;

import Timey.domain.location.LocationResolution;

/** Resolves a user-entered Singapore location into coordinates. */
public interface LocationResolver {
    LocationResolution resolve(String query);
}
