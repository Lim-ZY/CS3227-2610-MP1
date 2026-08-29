package timey.ports;

import java.time.LocalDate;
import java.time.LocalTime;

import timey.domain.location.ResolvedLocation;
import timey.domain.transit.LiveRouteLookup;

/** Finds live rail-only public-transport route alternatives. */
public interface RailTransitPlanner {
    LiveRouteLookup findRoutes(ResolvedLocation origin, ResolvedLocation destination,
            LocalDate departureDate, LocalTime departureTime);
}
