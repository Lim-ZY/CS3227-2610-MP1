package Timey.ports;

import java.util.List;

import Timey.domain.transit.RouteAlternative;

/** Finds transit route alternatives without exposing a particular provider. */
public interface TransitPlanner {
    List<RouteAlternative> findRoutes(String origin, String destination);
}
