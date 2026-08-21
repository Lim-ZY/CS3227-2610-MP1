package Timey.ports;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.RouteAlternative;

/** Finds live rail-only public-transport route alternatives. */
public interface RailTransitPlanner {
    List<RouteAlternative> findRoutes(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime);
}
