package Timey.ports;

import java.time.LocalDate;
import java.time.LocalTime;
import Timey.domain.location.ResolvedLocation;
import Timey.domain.transit.LiveRouteLookup;

/** Finds live rail-only public-transport route alternatives. */
public interface RailTransitPlanner {
    LiveRouteLookup findRoutes(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime);
}
