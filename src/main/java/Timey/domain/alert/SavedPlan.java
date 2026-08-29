package Timey.domain.alert;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** A selected commute plan ready for local persistence. */
public record SavedPlan(LocalDate date, LocalTime arrivalTime, String origin, String destination, LocalTime leaveBy) {
    public SavedPlan {
        Objects.requireNonNull(date, "Date must be provided.");
        Objects.requireNonNull(arrivalTime, "Arrival time must be provided.");
        Objects.requireNonNull(leaveBy, "Leave-by time must be provided.");
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("Origin must not be blank.");
        }
        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Destination must not be blank.");
        }
    }
}
